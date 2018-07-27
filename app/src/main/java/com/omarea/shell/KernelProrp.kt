package com.omarea.shell

/**
 * 操作内核参数节点
 * Created by Hello on 2017/11/01.
 */
object KernelProrp {
    /**
     * 获取属性
     * @param propName 属性名称
     * @return
     */
    fun getProp(propName: String): String {
        return KeepShellSync.doCmdSync("if [ -e \"$propName\" ]; then\ncat \"$propName\";\nfi;")
    }
    fun getProp(propName: String, grep: String): String {
        return KeepShellSync.doCmdSync("if [ -e \"$propName\" ]; then\ncat $propName | grep \"$grep\";\nfi;")
    }

    /**
     * 保存属性
     * @param propName 属性名称（要永久保存，请以persist.开头）
     * @param value    属性值,值尽量是简单的数字或字母，避免出现错误
     */
    fun setProp(propName: String, value: String): Boolean {
        return KeepShellSync.doCmdSync("echo \"\$value\" > $propName") != "error"
    }
}