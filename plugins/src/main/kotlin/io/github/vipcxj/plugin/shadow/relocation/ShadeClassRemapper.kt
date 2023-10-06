package io.github.vipcxj.plugin.shadow.relocation

import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.commons.ClassRemapper

class ShadeClassRemapper(
    classVisitor: ClassVisitor,
    private val pkg: String,
    private val packageMapper: DefaultPackageMapper) : ClassRemapper(classVisitor, LazyInitRemapper()), PackageMapper {

        init {
            LazyInitRemapper::class.java.cast(remapper).relocators = this
        }

    private var remapped: Boolean = false

    override fun visitSource(source: String?, debug: String?) {
        if (source == null) {
            super.visitSource(null, debug)
            return
        }
        val fqSource = pkg + source
        val mappedSource = map(fqSource, true, false)
        val filename = mappedSource.substring(mappedSource.lastIndexOf('/') + 1)
        super.visitSource(filename, debug)
    }

    override fun map(entityName: String, mapPaths: Boolean, mapPackages: Boolean): String {
        val mapped = packageMapper.map(entityName, true, mapPackages)
        if (!remapped) {
            remapped = mapped != entityName
        }
        return mapped
    }
}