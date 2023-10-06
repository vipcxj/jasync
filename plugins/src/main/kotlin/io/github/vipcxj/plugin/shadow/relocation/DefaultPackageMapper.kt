package io.github.vipcxj.plugin.shadow.relocation

import java.util.regex.Matcher
import java.util.regex.Pattern

class DefaultPackageMapper(private val relocators: List<Relocator>) : PackageMapper {

    companion object {
        private val CLASS_PATTERN: Pattern = Pattern.compile("(\\[*)?L(.+);")
    }

    override fun map(entityName: String, mapPaths: Boolean, mapPackages: Boolean): String {
        var en = entityName
        var value = en
        var prefix = ""
        var suffix = ""
        val m: Matcher = CLASS_PATTERN.matcher(en)
        if (m.matches()) {
            prefix = m.group(1) + "L"
            suffix = ";"
            en = m.group(2)
        }
        for (r in relocators) {
            if (mapPackages && r.canRelocateClass(en)) {
                value = prefix + r.relocateClass(en) + suffix
                break
            } else if (mapPaths && r.canRelocatePath(en)) {
                value = prefix + r.relocatePath(en) + suffix
                break
            }
        }
        return value
    }
}