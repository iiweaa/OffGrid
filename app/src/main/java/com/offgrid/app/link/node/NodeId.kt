package com.offgrid.app.link.node

import java.security.SecureRandom

/**
 * 64-bit mesh node identifier.
 *
 * A NodeId is stable for the lifetime of the app install. It is represented as
 * an unsigned 64-bit value stored in a [Long] for efficient serialization.
 */
@JvmInline
value class NodeId(val value: Long) {

    /**
     * Returns the 16-character lowercase hexadecimal representation.
     */
    fun toHex(): String = value.toULong().toString(16).padStart(HEX_LENGTH, '0')

    companion object {
        const val SIZE_BYTES = 8
        const val HEX_LENGTH = 16

        private val secureRandom = SecureRandom()

        fun random(): NodeId {
            var id = secureRandom.nextLong()
            // Extremely unlikely, but avoid the all-zero identifier.
            while (id == 0L) {
                id = secureRandom.nextLong()
            }
            return NodeId(id)
        }

        fun parse(hex: String): NodeId? {
            return try {
                if (hex.length != HEX_LENGTH) return null
                NodeId(hex.toULong(16).toLong())
            } catch (_: NumberFormatException) {
                null
            }
        }
    }
}
