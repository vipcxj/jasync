package io.github.vipcxj.plugin.shadow.relocation

import org.objectweb.asm.commons.Remapper

class LazyInitRemapper : Remapper() {

    var relocators: PackageMapper? = null

    override fun mapValue(`object`: Any?): Any {
        return if (`object` is String) relocators!!.map((`object` as String?)!!, true, true) else super.mapValue(
            `object`
        )
    }

    override fun map(name: String?): String {
        // NOTE: Before the factoring out duplicate code from 'private String map(String, boolean)', this method did
        // the same as 'mapValue', except for not trying to replace "dotty" package-like patterns (only "slashy"
        // path-like ones). The refactoring retains this difference. But actually, all unit and integration tests
        // still pass, if both variants are unified into one which always tries to replace both pattern types.
        //
        //  TODO: Analyse if this case is really necessary and has any special meaning or avoids any known problems.
        //   If not, then simplify DefaultShader.PackageMapper.map to only have the String parameter and assume
        //   both boolean ones to always be true.
        return relocators!!.map(name!!, true, false)
    }
}