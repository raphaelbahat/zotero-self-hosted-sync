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
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.immutable.reference.ImmutableMethodReference

/** The client the attachment upload is sent with. */
private const val MODULE_CLASS = "Lorg/zotero/android/api/module/NonZoteroApiModule;"

/** The last call in that client's chain, and the call that ends it. */
private const val BUILDER_CLASS = "Lokhttp3/OkHttpClient\$Builder;"
private const val ADD_INTERCEPTOR = "addInterceptor"
private const val INTERCEPTOR_CLASS = "Lokhttp3/Interceptor;"
private const val OKHTTP_CLIENT_CLASS = "Lokhttp3/OkHttpClient;"
private const val BUILD = "build"

/** The replacement. */
private const val EXTENSION_CLASS =
    "Lapp/anondev/patches/zotero/extension/AttachmentUploadInterceptor;"
private const val EXTENSION_METHOD = "install"

/** How far after the interceptor the chain may still be closed. */
private const val MAX_STEPS_TO_BUILD = 6

/**
 * Sends an attachment upload as the file, not as a form wrapped around it.
 *
 * The client uploads in the S3 shape it is written for: a `multipart/form-data` body of the upload
 * form's fields plus one `file` part. A server that takes the bytes itself — because it is not S3
 * and handed out no upload form — reads the body as the file and holds it to the length and digest
 * the upload was authorized for, so the form around it makes the upload fail. The desktop client,
 * which sends the file on its own in that case, is unaffected.
 *
 * The interceptor added here rewrites only that shape: a multipart body whose sole part is the
 * `file` part. An upload that carries a form has more than one part and passes through untouched.
 *
 * It is installed by replacing the client's last `addInterceptor` call, because that call takes and
 * returns exactly what the replacement needs — the builder and an interceptor in, the builder out —
 * so the chain keeps its order and no instruction has to be inserted and no register found.
 */
@Suppress("unused")
val attachmentUploadEnvelopePatch = bytecodePatch(
    name = "Send attachment uploads as the file itself",
    description = "Temporary workaround: sends an attachment upload without the multipart " +
        "form around it when the server authorized the upload without an S3 form. For servers " +
        "that take the bytes themselves; uploads that carry a form are unaffected.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_ZOTERO)
    category("Sync")

    extendWith("extensions/extension.mpe")

    execute {
        val replacement = ImmutableMethodReference(
            EXTENSION_CLASS,
            EXTENSION_METHOD,
            listOf(BUILDER_CLASS, INTERCEPTOR_CLASS),
            BUILDER_CLASS,
        )

        var replacements = 0
        mutableClassDefBy(MODULE_CLASS).methods.forEach { method ->
            val instructions = method.implementation?.instructions ?: return@forEach

            instructions.forEachIndexed { index, instruction ->
                if (instruction.opcode != Opcode.INVOKE_VIRTUAL) return@forEachIndexed
                if (instruction !is ReferenceInstruction) return@forEachIndexed
                if (instruction !is FiveRegisterInstruction) return@forEachIndexed
                if (instruction.registerCount != 2) return@forEachIndexed

                val reference = instruction.reference
                if (reference !is MethodReference) return@forEachIndexed
                if (reference.definingClass != BUILDER_CLASS) return@forEachIndexed
                if (reference.name != ADD_INTERCEPTOR) return@forEachIndexed
                if (reference.parameterTypes != listOf(INTERCEPTOR_CLASS)) return@forEachIndexed
                if (reference.returnType != BUILDER_CLASS) return@forEachIndexed

                // The last interceptor is the one the chain is closed after; that is this client's.
                if (!closesWith(method, instructions, index, instruction.registerC)) {
                    return@forEachIndexed
                }

                method.replaceInstruction(
                    index,
                    BuilderInstruction35c(
                        Opcode.INVOKE_STATIC,
                        2,
                        instruction.registerC,
                        instruction.registerD,
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
                "Send attachment uploads as the file: expected exactly one closing " +
                    "'$ADD_INTERCEPTOR' in $MODULE_CLASS, found $replacements. The patch targets " +
                    "Zotero 1.0.0-247.",
            )
        }
    }
}

/**
 * Whether the client built by [builderRegister] is finished off shortly after [index].
 *
 * The class adds two interceptors; only the one the chain is closed after is the client this patch
 * belongs on, and the other must be left where it is.
 */
private fun closesWith(
    method: com.android.tools.smali.dexlib2.iface.Method,
    instructions: List<com.android.tools.smali.dexlib2.iface.instruction.Instruction>,
    index: Int,
    builderRegister: Int,
): Boolean {
    val last = minOf(index + MAX_STEPS_TO_BUILD, instructions.size - 1)
    for (position in index + 1..last) {
        val instruction = instructions[position]
        if (instruction.opcode != Opcode.INVOKE_VIRTUAL) continue
        if (instruction !is ReferenceInstruction) continue
        if (instruction !is FiveRegisterInstruction) continue

        val reference = instruction.reference
        if (reference !is MethodReference) continue
        if (reference.definingClass != BUILDER_CLASS) continue
        if (reference.name != BUILD) continue
        if (reference.returnType != OKHTTP_CLIENT_CLASS) continue
        if (instruction.registerCount != 1) continue
        if (instruction.registerC == builderRegister) return true
    }
    return false
}