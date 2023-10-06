package io.github.vipcxj.plugin.shadow.relocation

interface Relocator {

    companion object {
        var ROLE = Relocator::class.java.getName()
    }

    fun canRelocatePath(clazz: String?): Boolean

    fun relocatePath(clazz: String?): String?

    fun canRelocateClass(clazz: String?): Boolean

    fun relocateClass(clazz: String?): String?

    fun applyToSourceContent(sourceContent: String?): String?
}