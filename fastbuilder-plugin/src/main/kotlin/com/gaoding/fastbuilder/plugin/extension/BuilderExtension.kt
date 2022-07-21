package com.gaoding.fastbuilder.plugin.extension

open class BuilderExtension {

    var showLog: Boolean = true
    var enable = true
    var applicationName: String = ""

    fun getNameSet(): HashSet<String> {
        val nameSet: HashSet<String> = HashSet()

        nameSet.add(applicationName)
        return nameSet
    }

    override fun toString(): String {
        return "BuilderExtension(showLog=$showLog, enable=$enable, applicationName='$applicationName')"
    }


}
