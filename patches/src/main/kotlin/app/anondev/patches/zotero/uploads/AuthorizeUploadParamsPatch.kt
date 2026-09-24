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

/** The parser that decides whether an authorized upload can be sent at all. */
private const val COMPANION_CLASS =
    "Lorg/zotero/android/sync/syncactions/data/AuthorizeNewUploadResponse\$Companion;"

/** The field being read, and the read being replaced. */
private const val PARAMS_KEY = "params"
private const val JSON_OBJECT_CLASS = "Lcom/google/gson/JsonObject;"
private const val JSON_ELEMENT_CLASS = "Lcom/google/gson/JsonElement;"
private const val JSON_GET = "get"

/** The replacement. */
private const val EXTENSION_CLASS =
    "Lapp/anondev/patches/zotero/extension/AuthorizeUploadParams;"
private const val EXTENSION_METHOD = "orEmpty"

/**
 * Accepts an upload authorization that carries no upload form.
 *
 * The client reads `params` from the authorization response and parses it into a map, treating the
 * result as non-null (`!!`). A server that hands out no upload form — because it takes the bytes
 * itself instead of relaying them to S3 — sends no `params` field at all, so the parse yields null
 * and the client throws `NullPointerException` from its own parser. The attachment is then never
 * uploaded, the sync retries the whole authorization, and the user is shown a failure with no
 * explanation. The desktop client accepts the same response.
 *
 * Only the read is replaced: the element handed to the parser is the response's `params` when it
 * has one and an empty envelope when it does not, so an authorization that does carry a form is
 * parsed exactly as before.
 *
 * The replacement is scoped to the parser's companion class and matched on the `"params"` key that
 * immediately precedes the read, so no other `JsonObject.get` in the app is touched. Anything other
 * than exactly one match is a hard failure rather than a guess.
 */
@Suppress("unused")
val authorizeUploadParamsPatch = bytecodePatch(
    name = "Accept upload authorization that carries no upload form",
    description = "Uploads the file when the sync server authorizes an upload without an S3 form " +
        "envelope. Without this, an authorization the server completed is thrown away as a parse " +
        "error and the attachment never uploads.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_ZOTERO)
    category("Sync")

    extendWith("extensions/extension.mpe")

    execute {
        val replacement = ImmutableMethodReference(
            EXTENSION_CLASS,
            EXTENSION_METHOD,
            listOf(JSON_OBJECT_CLASS),
            JSON_ELEMENT_CLASS,
        )

        var replacements = 0
        mutableClassDefBy(COMPANION_CLASS).methods.forEach { method ->
            val instructions = method.implementation?.instructions ?: return@forEach

            instructions.forEachIndexed { index, instruction ->
                if (instruction.opcode != Opcode.CONST_STRING) return@forEachIndexed
                if (instruction !is ReferenceInstruction) return@forEachIndexed
                if (instruction !is OneRegisterInstruction) return@forEachIndexed

                val key = instruction.reference
                if (key !is StringReference) return@forEachIndexed
                if (key.string != PARAMS_KEY) return@forEachIndexed

                // The read is the next call, and it reads from the register the key was loaded into.
                if (index + 1 >= instructions.size) return@forEachIndexed
                val read = instructions[index + 1]
                if (read.opcode != Opcode.INVOKE_VIRTUAL) return@forEachIndexed
                if (read !is ReferenceInstruction) return@forEachIndexed
                if (read !is FiveRegisterInstruction) return@forEachIndexed
                if (read.registerCount != 2) return@forEachIndexed

                val reference = read.reference
                if (reference !is MethodReference) return@forEachIndexed
                if (reference.definingClass != JSON_OBJECT_CLASS) return@forEachIndexed
                if (reference.name != JSON_GET) return@forEachIndexed
                if (reference.parameterTypes != listOf("Ljava/lang/String;")) return@forEachIndexed
                if (reference.returnType != JSON_ELEMENT_CLASS) return@forEachIndexed
                if (read.registerD != instruction.registerA) return@forEachIndexed

                // Reuse the receiver register: the helper takes the response object the read did.
                method.replaceInstruction(
                    index + 1,
                    BuilderInstruction35c(
                        Opcode.INVOKE_STATIC,
                        1,
                        read.registerC,
                        0,
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
                "Accept upload authorization: expected exactly one '$PARAMS_KEY' read in " +
                    "$COMPANION_CLASS, found $replacements. The patch targets Zotero 1.0.0-247.",
            )
        }
    }
}
