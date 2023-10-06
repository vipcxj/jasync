package io.github.vipcxj.plugin.shadow.relocation

interface PackageMapper {

    /**
     * Map an entity name according to the mapping rules known to this package mapper
     *
     * @param entityName entity name to be mapped
     * @param mapPaths map "slashy" names like paths or internal Java class names, e.g. `com/acme/Foo`?
     * @param mapPackages  map "dotty" names like qualified Java class or package names, e.g. `com.acme.Foo`?
     * @return mapped entity name, e.g. `org/apache/acme/Foo` or `org.apache.acme.Foo`
     */
    fun map(entityName: String, mapPaths: Boolean, mapPackages: Boolean): String
}