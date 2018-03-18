package com.omarea.vboot

import android.os.Bundle
import android.os.Handler
import android.support.design.widget.Snackbar
import android.support.v4.app.Fragment
import android.support.v7.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.omarea.shared.AppShared
import com.omarea.shared.Consts
import com.omarea.shell.SuDo
import com.omarea.shell.SysUtils
import com.omarea.shell.units.FlymeUnit
import com.omarea.shell.units.FullScreenSUnit
import com.omarea.shell.units.QQStyleUnit
import com.omarea.ui.ProgressBarDialog
import com.omarea.vboot.addin.DexCompileAddin
import com.omarea.vboot.dialogs.DialogAddinModifyDPI
import com.omarea.vboot.dialogs.DialogAddinModifydevice
import com.omarea.vboot.dialogs.DialogAddinWIFI
import com.omarea.vboot.dialogs.DialogCustomMAC
import kotlinx.android.synthetic.main.layout_addin.*
import java.io.File
import java.util.*


class FragmentAddin : Fragment() {
    internal var thisview: ActivityMain? = null
    internal val myHandler: Handler = Handler()
    private lateinit var processBarDialog: ProgressBarDialog

    private fun createItem(title: String, desc: String): HashMap<String, Any> {
        val item = HashMap<String, Any>()
        item.put("Title", title)
        item.put("Desc", desc)
        return item
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? =
            inflater!!.inflate(R.layout.layout_addin, container, false)


    private fun initSoftAddin(view: View) {
        val listView = view.findViewById(R.id.addin_soft_listview) as ListView
        val listItem = ArrayList<HashMap<String, Any>>()/*在数组中存放数据*/

        listItem.add(createItem("QQ净化", "干掉QQ个性气泡、字体、头像挂件，会重启QQ"))
        listItem.add(createItem("QQ净化恢复", "恢复QQ个性气泡、字体、头像挂件，会重启QQ"))
        listItem.add(createItem("开启沉浸模式", "自动隐藏状态栏、导航栏"))
        listItem.add(createItem("禁用沉浸模式", "恢复状态栏、导航栏自动显示"))
        listItem.add(createItem("减少Flyme6模糊", "禁用Flyme6下拉通知中心的实时模糊效果，以减少在游戏或视频播放时下拉通知中心的卡顿，或许还能省电"))
        listItem.add(createItem("MIUI9去通知中心搜索", "默认隐藏MIUI9系统下拉通知中心的搜索框，重启后生效"))
        listItem.add(createItem("隐藏!和×", "禁用网络可用性检测，去除类原生系统状态栏上WIIF、4G图标的!和x"))
        listItem.add(createItem("冻结谷歌套件", "冻结谷歌服务基础4件套，以减少待机耗电"))
        listItem.add(createItem("解冻谷歌套件", "解冻谷歌4件套"))

        val mSimpleAdapter = SimpleAdapter(
                view.context, listItem,
                R.layout.action_row_item,
                arrayOf("Title", "Desc"),
                intArrayOf(R.id.Title, R.id.Desc)
        )
        listView.adapter = mSimpleAdapter


        listView.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            val builder = AlertDialog.Builder(thisview!!)
            builder.setTitle("执行这个脚本？")
            builder.setNegativeButton(android.R.string.cancel, null)
            builder.setPositiveButton(android.R.string.yes) { _, _ -> executeSoftScript(view, position) }
            builder.setMessage(
                    listItem[position]["Title"].toString() + "：" + listItem[position]["Desc"] +
                            "\n\n请确保你已了解此脚本的用途，并清楚对设备的影响")
            builder.create().show()
        }
    }

    private fun executeSoftScript(view: View, position: Int) {
        val stringBuilder = StringBuilder()
        when (position) {
            0 -> {
                QQStyleUnit().DisableQQStyle()
                return
            }
            1 -> {
                QQStyleUnit().RestoreQQStyle()
                return
            }
            2 -> {
                FullScreenSUnit().FullScreen()
                return
            }
            3 -> {
                FullScreenSUnit().ExitFullScreen()
                return
            }
            4 -> {
                FlymeUnit().StaticBlur()
                return
            }
            5 -> {
                AppShared.WritePrivateFile(context.assets, "com.android.systemui", "com.android.systemui", context)
                stringBuilder.append(Consts.MountSystemRW)
                stringBuilder.append("cp ${AppShared.getPrivateFileDir(context)}/com.android.systemui /system/media/theme/default/com.android.systemui\n")
                stringBuilder.append("chmod 0644 /system/media/theme/default/com.android.systemui\n")
            }
            6 -> {
                stringBuilder.append("settings put global airplane_mode_on 1;")
                stringBuilder.append("am broadcast -a android.intent.action.AIRPLANE_MODE --ez state true;")
                stringBuilder.append("settings put global captive_portal_mode 0;")
                stringBuilder.append("settings put global captive_portal_detection_enabled 0;")
                stringBuilder.append("settings put global captive_portal_server www.androidbak.net;")
                stringBuilder.append("settings put global airplane_mode_on 0;")
                stringBuilder.append("am broadcast -a android.intent.action.AIRPLANE_MODE --ez state false;");
            }
            7 -> {
                stringBuilder.append("pm disable com.google.android.gsf;")
                stringBuilder.append("pm disable com.google.android.gsf.login;")
                stringBuilder.append("pm disable com.google.android.gms;")
                stringBuilder.append("pm disable com.android.vending;")
            }
            8 -> {
                stringBuilder.append("pm enable com.google.android.gsf;")
                stringBuilder.append("pm enable com.google.android.gsf.login;")
                stringBuilder.append("pm enable com.google.android.gms;")
                stringBuilder.append("pm enable com.android.vending;")
            }
        }
        processBarDialog.showDialog("正在执行，请稍等...")
        Thread(Runnable {
            SuDo(context).execCmdSync(stringBuilder.toString())
            myHandler.post {
                Snackbar.make(view, "命令已执行！", Snackbar.LENGTH_SHORT).show()
                processBarDialog.hideDialog()
            }
        }).start()
    }

    private fun getIP () : String {
        var r = SysUtils.executeCommandWithOutput(false, "ifconfig wlan0 | grep \"inet addr\" | awk '{ print \$2}' | awk -F: '{print \$2}'")
        if (r ==null || r== "") {
            r = "手机IP"
        }
        return r.trim()
    }

    private fun initSystemAddin(view: View) {
        val listView = view.findViewById(R.id.addin_system_listview) as ListView
        val listItem = ArrayList<HashMap<String, Any>>()/*在数组中存放数据*/

        listItem.add(createItem("内存清理", "Linux标准缓存清理命令：echo 3 > /proc/sys/vm/drop_caches"))
        listItem.add(createItem("干掉温控模块", "这可以确保手机性能更加稳定，但会显著增加发热，同时也会导致MIUI系统的CPU智能调度失效，需要重启手机。"))
        listItem.add(createItem("恢复温控模块", "如果你后悔了想把温控还原回来，可以点这个。需要重启手机"))
        listItem.add(createItem("关闭温控", "临时关闭温控，不需要重启手机，重启后失效"))
        listItem.add(createItem("删除锁屏密码", "如果你忘了锁屏密码，或者恢复系统后密码不正确，这能帮你解决。会重启手机"))
        listItem.add(createItem("禁止充电", "停止对电池充电，同时使用USB电源为手机供电。（与充电加速和电池保护功能冲突！）"))
        listItem.add(createItem("恢复充电", "恢复对电池充电，由设备自行管理充放。"))
        listItem.add(createItem("改机型为OPPO R11", "将机型信息改为OPPO R11 Plus，以便在王者荣耀或获得专属优化体验。（会导致部分设备软件fc）"))
        listItem.add(createItem("改机型为vivo X20", "将机型信息改为vivo X20，据说比改OPPO R11还好用？（会导致部分设备软件fc）"))
        listItem.add(createItem("build.prop参数还原", "使用了DPI修改和机型伪装的小伙伴，可以点这个还原到上次修改前的状态"))
        listItem.add(createItem("开启网络ADB", "开启网络adb前，请先连接wifi。开启后通过同一局域网下的电脑，使用adb connect ${getIP()}:5555 命令连接"))
        listItem.add(createItem("查看保存的WIFI", "查看已保存的WIFI信息，通过读取/data/misc/wifi/wpa_supplicant.conf 或 /data/misc/wifi/WifiConfigStore.xml（Android Oreo+）"))
        listItem.add(createItem("强制编译Dex", "适用于Android N+，对已安装应用进行完整的dex2oat编译，从而提高运行性能。首次执行此操作，可能需要几十分钟，并增加应用空间占用30%左右，期间手机可能会卡顿，会消耗大量电量！！！"))
        listItem.add(createItem("调整Dex编译策略", "适用于Android N+，调整应用编译策略，可以选择更快的安装应用或缓慢的安装更流畅的运行。"))
        listItem.add(createItem("删除电池使用记录", "通过删除/data/system/batterystats-checkin.bin、/data/system/batterystats-daily.xml、/data/system/batterystats.bin 来清空系统的电池使用记录，会重启手机！"))


        val mSimpleAdapter = SimpleAdapter(
                view.context, listItem,
                R.layout.action_row_item,
                arrayOf("Title", "Desc"),
                intArrayOf(R.id.Title, R.id.Desc)
        )
        listView.adapter = mSimpleAdapter


        listView.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            if (position == 1 || position == 2 || position == 3) {
                if (File("/system/vendor/bin/thermal-engine").exists() || File("/system/vendor/bin/thermal-engine.bak").exists()) {
                    if (position == 3) {

                    } else {
                        if (position == 1 && File("/system/vendor/bin/thermal-engine.bak").exists()) {
                            Toast.makeText(context, "你已执行过这个操作，不需要再次执行，如果未生效请重启手机！", Toast.LENGTH_SHORT).show()
                            return@OnItemClickListener
                        }
                        if (position == 2 && File("/system/vendor/bin/thermal-engine").exists()) {
                            Toast.makeText(context, "你不需要此操作，温控文件正在正常使用，如果无效请重启手机！", Toast.LENGTH_SHORT).show()
                            return@OnItemClickListener
                        }
                    }
                } else {
                    Toast.makeText(context, "该功能暂不支持您的设备！", Toast.LENGTH_SHORT).show()
                    return@OnItemClickListener
                }
            }

            else if (position == 5 || position == 6) {
                if (File("/sys/class/power_supply/battery/battery_charging_enabled").exists() || File("/sys/class/power_supply/battery/input_suspend").exists()) {
                } else {
                    Toast.makeText(context, "该功能暂不支持您的设备！", Toast.LENGTH_SHORT).show()
                    return@OnItemClickListener
                }
            }
            else if (position == 11) {
                DialogAddinWIFI(context).show()
                return@OnItemClickListener
            }
            val builder = AlertDialog.Builder(thisview!!)
            builder.setTitle("执行这个脚本？")
            builder.setNegativeButton(android.R.string.cancel, null)
            builder.setPositiveButton(android.R.string.yes) { _, _ -> executeSystemScript(view, position) }
            builder.setMessage(
                    listItem[position]["Title"].toString() + "：" + listItem[position]["Desc"] +
                            "\n\n请确保你已了解此脚本的用途，并清楚对设备的影响")
            builder.create().show()
        }
    }

    private fun runCommand(stringBuilder: StringBuilder) {
        processBarDialog.showDialog("正在执行，请稍等...")
        Thread(Runnable {
            SuDo(context).execCmdSync(stringBuilder.toString())
            myHandler.post {
                Snackbar.make(this.view!!, "命令已执行！", Snackbar.LENGTH_SHORT).show()
                processBarDialog.hideDialog()
            }
        }).start()
    }

    private fun executeSystemScript(view: View, position: Int) {
        val stringBuilder = StringBuilder()
        when (position) {
            0 -> {
                stringBuilder.append("echo 3 > /proc/sys/vm/drop_caches")
            }
            1 -> {
                stringBuilder.append(Consts.MountSystemRW)
                stringBuilder.append(Consts.RMThermal)
            }
            2 -> {
                stringBuilder.append(Consts.MountSystemRW)
                stringBuilder.append(Consts.ResetThermal)
            }
            3 -> {
                stringBuilder.append("stop thermald;")
                stringBuilder.append("stop mpdecision;")
                stringBuilder.append("stop thermal-engine;")
                stringBuilder.append("echo 0 > /sys/module/msm_thermal/core_control/enabled;")
                stringBuilder.append("echo 0 > /sys/module/msm_thermal/vdd_restriction/enabled;")
                stringBuilder.append("echo N > /sys/module/msm_thermal/parameters/enabled;")
            }
            4 -> {
                stringBuilder.append(Consts.DeleteLockPwd)
            }
            5 -> {
                stringBuilder.append(Consts.DisableChanger)
            }
            6 -> {
                stringBuilder.append(Consts.ResumeChanger)
            }
            7 -> {
                stringBuilder.append(Consts.MountSystemRW)
                stringBuilder.append(
                                "busybox sed 's/^ro.product.model=.*/ro.product.model=OPPO R11 Plus/' /system/build.prop > /data/build.prop;" +
                                "busybox sed -i 's/^ro.product.brand=.*/ro.product.brand=OPPO/' /data/build.prop;" +
                                "busybox sed -i 's/^ro.product.name=.*/ro.product.name=R11 Plus/' /data/build.prop;" +
                                "busybox sed -i 's/^ro.product.device=.*/ro.product.device=R11 Plus/' /data/build.prop;" +
                                "busybox sed -i 's/^ro.build.product=.*/ro.build.product=R11 Plus/' /data/build.prop;" +
                                "busybox sed -i 's/^ro.product.manufacturer=.*/ro.product.manufacturer=OPPO/' /data/build.prop;")
                stringBuilder.append("cp /system/build.prop /system/build.bak.prop\n")
                stringBuilder.append("cp /data/build.prop /system/build.prop\n")
                stringBuilder.append("rm /data/build.prop\n")
                stringBuilder.append("chmod 0644 /system/build.prop\n")
                stringBuilder.append(Consts.Reboot)
            }
            8 -> {
                stringBuilder.append(Consts.MountSystemRW)
                stringBuilder.append(
                        "busybox sed 's/^ro.product.model=.*/ro.product.model=vivo X20/' /system/build.prop > /data/build.prop;" +
                        "busybox sed -i 's/^ro.product.brand=.*/ro.product.brand=vivo/' /data/build.prop;" +
                        "busybox sed -i 's/^ro.product.name=.*/ro.product.name=X20/' /data/build.prop;" +
                        "busybox sed -i 's/^ro.product.device=.*/ro.product.device=X20/' /data/build.prop;" +
                        "busybox sed -i 's/^ro.build.product=.*/ro.build.product=X20/' /data/build.prop;" +
                        "busybox sed -i 's/^ro.product.manufacturer=.*/ro.product.manufacturer=vivo/' /data/build.prop;")
                stringBuilder.append("cp /system/build.prop /system/build.bak.prop\n")
                stringBuilder.append("cp /data/build.prop /system/build.prop\n")
                stringBuilder.append("rm /data/build.prop\n")
                stringBuilder.append("chmod 0644 /system/build.prop\n")
                stringBuilder.append(Consts.Reboot)
            }
            9 -> {
                stringBuilder.append(Consts.MountSystemRW)
                stringBuilder.append("if [ -f '/system/build.bak.prop' ];then rm /system/build.prop;cp /system/build.bak.prop /system/build.prop;chmod 0644 /system/build.prop; sync;sleep 2;reboot; fi;")
            }
            10 -> {
                stringBuilder.append("setprop service.adb.tcp.port 5555;")
                stringBuilder.append("stop adbd;")
                stringBuilder.append("sleep 1;")
                stringBuilder.append("start adbd;")
            }
            12 -> {
                DexCompileAddin(context).run()
                return
            }
            13 -> {
                DexCompileAddin(context).modifyConfig()
                return
            }
            14 -> {
                stringBuilder.append("rm -f /data/system/batterystats-checkin.bin;rm -f /data/system/batterystats-daily.xml;rm -f /data/system/batterystats.bin;")
                stringBuilder.append(Consts.Reboot)
            }
        }
        runCommand(stringBuilder)
    }

    private fun initCustomAddin(view: View) {
        val listItem = ArrayList<HashMap<String, Any>>()/*在数组中存放数据*/
        listItem.add(createItem("DPI、分辨率修改", "自定义手机DPI或分辨率，这可能导致设备无法正常启动或UI错误"))
        listItem.add(createItem("机型修改", "通过更改build.prop，把机型修改成别的手机，可能会导致部分系统不能开机或出现Bug"))
        listItem.add(createItem("修改MAC地址", "自定义手机WIFI网卡MAC地址"))

        val mSimpleAdapter = SimpleAdapter(
                view.context, listItem,
                R.layout.action_row_item,
                arrayOf("Title", "Desc"),
                intArrayOf(R.id.Title, R.id.Desc)
        )
        addin_custom_listview.adapter = mSimpleAdapter


        addin_custom_listview.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            if (position == 0) {
                DialogAddinModifyDPI(context).modifyDPI(thisview!!.windowManager.defaultDisplay)
            } else if (position == 1) {
                DialogAddinModifydevice(context).modifyDeviceInfo()
            } else if (position == 2) {
                DialogCustomMAC(context).modifyMAC()
            }
        }
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        processBarDialog = ProgressBarDialog(this.context)
        val tabHost = view!!.findViewById(R.id.addinlist_tabhost) as TabHost

        tabHost.setup()

        tabHost.addTab(tabHost.newTabSpec("def_tab").setContent(R.id.system).setIndicator("系统"))
        tabHost.addTab(tabHost.newTabSpec("game_tab").setContent(R.id.soft).setIndicator("软件"))
        tabHost.addTab(tabHost.newTabSpec("custom_tab").setContent(R.id.custom).setIndicator("高级"))
        tabHost.currentTab = 0

        initSystemAddin(view)
        initSoftAddin(view)
        initCustomAddin(view)
    }

    companion object {

        fun createPage(thisView: ActivityMain): Fragment {
            val fragment = FragmentAddin()
            fragment.thisview = thisView
            return fragment
        }
    }
}// Required empty public constructor
