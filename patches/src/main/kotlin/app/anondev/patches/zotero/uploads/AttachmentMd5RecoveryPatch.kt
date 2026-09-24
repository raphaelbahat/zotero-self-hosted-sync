/*
 * Copyright 2026 anondev.
 *
 * GPLv3. See the LICENSE file for details.
 */

package app.anondev.patches.zotero.uploads

import app.anondev.patches.zotero.shared.Constants.COMPATIBILITY_ZOTERO
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction35c
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.StringReference
import com.android.tools.smali.dexlib2.immutable.reference.ImmutableMethodReference

/** The one class whose decision to repair the attachment `md5` decides whether the file uploads. */
private const val READER_CLASS =
    "Lorg/zotero/android/database/requests/ReadAttachmentUploadsDbRequest;"

/** The comparison being replaced, and why it has to be identified by more than its method. */
private const val INTRINSICS_CLASS = "Lkotlin/jvm/internal/Intrinsics;"
private const val ARE_EQUAL = "areEqual"
private const val NULL_LITERAL = "null"

/** The repair the reader already performs, which tells the two comparisons apart. */
private const val FILE_STORE_CLASS = "Lorg/zotero/android/files/FileStore;"
private const val FILE_STORE_MD5 = "md5"
private const val FILE_TYPE = "Ljava/io/File;"

/** The replacement. */
private const val EXTENSION_CLASS = "Lapp/anondev/patches/zotero/extension/AttachmentMd5;"
private const val EXTENSION_METHOD = "unusable"
private const val STRING_TYPE = "Ljava/lang/String;"
private const val BOOLEAN_TYPE = "Z"

/** How far the repair may sit below the branch it is reached through. */
private const val MAX_STEPS_TO_REPAIR = 12

/**
 * Sends a usable MD5 for an attachment whose stored digest is not one.
 *
 * The reader repairs a stored `md5` only when it is the literal string `"null"`, and otherwise puts
 * the stored value straight into the upload-authorization request. A row whose field is *empty*
 * therefore sends `md5=` with nothing in it, which the server rejects with `400 md5 not provided`:
 * the file never uploads, the field is never repaired, and the attachment never appears anywhere
 * else. It is the same closed loop as an unusable `mtime`, one field over.
 *
 * Only the comparison is replaced here, with a helper that asks the protocol's own question — is
 * this a 32-character hex digest, and is there a file to recompute it from? The reader's existing
 * repair (recompute with `FileStore.md5`, then store it on the attachment) is left to do its job, so
 * the stored value is corrected rather than merely substituted for this request.
 *
 * The reader holds two `Intrinsics.areEqual` calls that compare a field against the shared constant
 * `"null"`. The second one — `backendMd5 == "null"`, which is correct — sits below a `check-cast` and
 * has no repair under it, so the comparison that reaches `FileStore.md5` is the one targeted, and a
 * match other than exactly one is a hard failure rather than a guess.
 */
@Suppress("unused")
val attachmentMd5RecoveryPatch = bytecodePatch(
    name = "Recover attachments with an unusable MD5",
    description = "Temporary workaround: uploads attachments whose stored MD5 is empty or " +
        "malformed, instead of sending an unusable digest the server rejects. Without this, the " +
        "attachment's file never uploads and never reaches other devices.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_ZOTERO)
    category("Sync")

    extendWith("extensions/extension.mpe")

    execute {
        val replacement = ImmutableMethodReference(
            EXTENSION_CLASS,
            EXTENSION_METHOD,
            listOf(STRING_TYPE, FILE_TYPE),
            BOOLEAN_TYPE,
        )

        var replacements = 0
        mutableClassDefBy(READER_CLASS).methods.forEach { method ->
            val instructions = method.implementation?.instructions ?: return@forEach

            instructions.forEachIndexed { index, instruction ->
                if (instruction.opcode != Opcode.INVOKE_STATIC) return@forEachIndexed
                if (instruction !is ReferenceInstruction) return@forEachIndexed
                if (instruction !is FiveRegisterInstruction) return@forEachIndexed

                val reference = instruction.reference
                if (reference !is MethodReference) return@forEachIndexed
                if (reference.definingClass != INTRINSICS_CLASS) return@forEachIndexed
                if (reference.name != ARE_EQUAL) return@forEachIndexed
                if (instruction.registerCount != 2) return@forEachIndexed

                // The compared value is the left operand; the right one is the `"null"` literal.
                val valueRegister = instruction.registerC
                val literalRegister = instruction.registerD

                if (index == 0) return@forEachIndexed
                val preceding = instructions[index - 1]
                if (preceding.opcode != Opcode.CONST_STRING) return@forEachIndexed
                if (preceding !is ReferenceInstruction) return@forEachIndexed
                if (preceding !is OneRegisterInstruction) return@forEachIndexed
                if (preceding.registerA != literalRegister) return@forEachIndexed
                val literal = preceding.reference
                if (literal !is StringReference) return@forEachIndexed
                if (literal.string != NULL_LITERAL) return@forEachIndexed

                // Only the comparison above a `FileStore.md5` repair is the one that guards it.
                val fileRegister = repairFileRegister(instructions, index)
                    ?: return@forEachIndexed

                method.replaceInstruction(
                    index,
                    BuilderInstruction35c(
                        Opcode.INVOKE_STATIC,
                        2,
                        valueRegister,
                        fileRegister,
                        0,
                        0,
                        0,
                        replacement,
                    ),
                )
                replacements++
            }
        }

        if (replacements != 1) {
            throw PatchException(
                "Recover attachments: expected exactly one md5 repair guard in $READER_CLASS, " +
                    "found $replacements. The patch targets Zotero 1.0.0-247.",
            )
        }
    }
}

/**
 * Returns the attachment-file register of the `FileStore.md5` call the comparison at [index] guards,
 * or null when the comparison is not the md5 repair.
 *
 * Both `Intrinsics.areEqual` calls in the reader compare a field against the same shared `"null"`
 * constant, and only the first one is followed by the repair that recomputes the digest from the
 * file. The repair names the file in the second argument of that call, which is what the replacement
 * has to hash.
 */
private fun repairFileRegister(
    instructions: List<com.android.tools.smali.dexlib2.iface.instruction.Instruction>,
    index: Int,
): Int? {
    val last = minOf(index + MAX_STEPS_TO_REPAIR, instructions.size - 1)
    for (position in index + 1..last) {
        val instruction = instructions[position]
        if (instruction.opcode == Opcode.INVOKE_VIRTUAL &&
            instruction is FiveRegisterInstruction &&
            instruction.registerCount == 2
        ) {
            val reference = (instruction as? ReferenceInstruction)?.reference
            if (reference is MethodReference &&
                reference.definingClass == FILE_STORE_CLASS &&
                reference.name == FILE_STORE_MD5 &&
                reference.parameterTypes == listOf(FILE_TYPE)
            ) {
                return instruction.registerD
            }
        }
    }
    return null
}
