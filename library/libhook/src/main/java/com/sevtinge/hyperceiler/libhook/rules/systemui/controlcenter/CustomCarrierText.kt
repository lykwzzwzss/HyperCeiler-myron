/*
 * This file is part of HyperCeiler.
 *
 * HyperCeiler is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (C) 2023-2026 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.libhook.rules.systemui.controlcenter

import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.isVisible
import com.sevtinge.hyperceiler.common.log.XposedLog
import com.sevtinge.hyperceiler.common.utils.PrefsBridge
import com.sevtinge.hyperceiler.libhook.base.BaseHook
import com.sevtinge.hyperceiler.libhook.utils.api.PropUtils
import io.github.lingqiqi5211.ezhooktool.core.callMethod
import io.github.lingqiqi5211.ezhooktool.core.callMethodAs
import io.github.lingqiqi5211.ezhooktool.core.findMethod
import io.github.lingqiqi5211.ezhooktool.core.loadClass
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createAfterHook
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createBeforeHook
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.getObjectFieldAs
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.setIntField

/**
 * OS3 控制中心运行商名称自定义
 */
object CustomCarrierText : BaseHook() {

    /** 实时读取（不缓存）：用户切换模式后立即生效，不依赖进程重启。 */
    val getOperator
        get() = PrefsBridge.getStringAsInt("system_ui_control_center_hide_operator", 0)

    override fun init() {
        loadClass("com.android.systemui.statusbar.policy.HDController").findMethod { name("isVisible") }.createBeforeHook { param ->
                param.result = false
            }

        hookLegacyCarrierText()
        hookModernCarrierText()
        hookControllerSource()
        hookForcedDisplay()

        when (getOperator) {
            1 -> hideCarrierSeparator()
            2 -> hideCarrierText()
        }
    }

    private fun hookLegacyCarrierText() {
        runCatching {
            loadClass("com.android.systemui.statusbar.phone.MiuiKeyguardStatusBarView").findMethod { name("onCarrierTextChanged") }.createBeforeHook { param ->
                    applyCarrierTextTransform(param.args)
                }
        }
    }

    // 显示设备名称
    private fun hookModernCarrierText() {
        // 三参事件路径
        runCatching {
            loadClass("com.android.systemui.controlcenter.shade.ControlCenterCarrierText" + 36.toChar() + "mCarrierTextCallback" + 36.toChar() + "1").findMethod { name("onCarrierTextChanged") }.createBeforeHook { param ->
                    applyCarrierTextTransform(param.args)
                }
        }
        // 二参初值/刷新路径：ICarrierTextListener.onCarrierTextChanged(I,String)V 的接口默认实现
        // 为空（smali 实测 return-void）且实现类未覆写——不单独挂载则该链路整条绕过文本替换，
        // 表现为「显示设备名称」模式不生效（模式 1/2 有独立 hook 兜底故不易察觉）。
        runCatching {
            loadClass("com.miui.interfaces.statusbar.ICarrierTextController" + 36.toChar() + "ICarrierTextListener")
                .findMethod { name("onCarrierTextChanged"); parameterTypes(Int::class.java, String::class.java) }
                .createBeforeHook { param ->
                    applyCarrierTextTransform(param.args)
                }
        }
    }

    /**
     * 显示真源：MiuiCarrierTextController 的 per-slot 文本字段（String[]，下标即 slotId）。
     *
     * smali 实测（系统界面 17.03.260226.r）：显示端 ControlCenterCarrierText 唯一文本入口是
     * 三参回调，而主刷新 updateCarrierText() 只发二参通知（接口默认实现为空）——回调层替换
     * 无法覆盖初值/刷新。故在字段层替换（updateCarrierText 的 StringBuilder 拼接输入即这些字段，
     * 与旧链路 HideDelimiter 的成熟修法同源）。
     */
    private fun hookControllerSource() {
        runCatching {
            loadClass("com.android.systemui.statusbar.policy.MiuiCarrierTextController")
                .findMethod { name("updateCarrierText") }
                .createBeforeHook { param ->
                    listOf("mCurrentCarrier", "mCustomCarrier", "mCarrier").forEach { field ->
                        runCatching {
                            val slots = param.thisObject.getObjectFieldAs<Array<String?>>(field)
                            val out = CarrierTextTransform.transformAll(getOperator, slots, deviceName)
                            for (i in slots.indices) slots[i] = out[i]
                        }
                    }
                }
        }
    }

    /**
     * 覆盖式显示兜底：在初值渲染（onFinishInflate）与三参刷新回调之后强制覆写
     * ControlCenterCarrierText 的 TextView 文本——系统无论何时 setText 都会被顶掉，
     * 不依赖其文本设置点的内部实现（该点经三轮静态排查未定位，改为覆盖式）。
     * 带插桩日志供真机对账。
     */
    private fun hookForcedDisplay() {
        val apply: (Any?) -> Unit = { self -> applyForcedText(self) }
        runCatching {
            loadClass("com.android.systemui.controlcenter.shade.ControlCenterCarrierText")
                .findMethod { name("onFinishInflate") }
                .createAfterHook { param -> apply(param.thisObject) }
        }
        runCatching {
            loadClass("com.android.systemui.controlcenter.shade.ControlCenterCarrierText" + 36.toChar() + "mCarrierTextCallback" + 36.toChar() + "1")
                .findMethod { name("onCarrierTextChanged") }
                .createAfterHook { param ->
                    apply(param.thisObject.getObjectFieldAs<Any>("this" + 36.toChar() + "0"))
                }
        }
    }

    private fun applyForcedText(self: Any?) {
        runCatching {
            val slotId = self!!.callMethodAs<Int>("getSlotId")
            val tv = self.callMethodAs<TextView>("getCarrierTextView")
            val orig = tv.text?.toString()
            val out = CarrierTextTransform.transform(getOperator, arrayOf(slotId, orig), deviceName)
            tv.text = out ?: ""
            XposedLog.d("CustomCarrierText", "forced slot=" + slotId + " mode=" + getOperator
                + " [" + orig + "] -> [" + out + "]")
        }.onFailure {
            XposedLog.e("CustomCarrierText", "applyForcedText failed: " + it)
        }
    }

    private val deviceName by lazy { PropUtils.getProp("persist.private.device_name") }

    /**
     * 把变换结果写回 onCarrierTextChanged 回调参数。
     *
     * 实测签名（系统界面 17.03.260226.r）：onCarrierTextChanged(I Ljava/lang/String; I)V
     * ——slotId 在 args[0]、运营商文本在 args[1]、status 在 args[2]。
     * 旧代码按 (x, Int, String) 取 args[1]/args[2] 会在新签名上抛 ClassCastException 导致整个功能失效。
     */
    private fun applyCarrierTextTransform(args: Array<Any?>) {
        CarrierTextTransform.applyTo(
            args,
            CarrierTextTransform.transform(getOperator, args, deviceName)
        )
    }

    // 隐藏分割线
    private fun hideCarrierSeparator() {
        loadClass("com.android.systemui.controlcenter.shade.MiuiCarrierTextLayout").findMethod { name("onMeasure"); parameterTypes(Int::class.java, Int::class.java) }.createBeforeHook { param ->
                val viewGroup = param.thisObject as ViewGroup
                val widthMeasureSpec = param.args[0] as Int
                val heightMeasureSpec = param.args[1] as Int

                if (!viewGroup.isVisible) {
                    // super.onMeasure
                    invokeSuperMethod(
                        "onMeasure", viewGroup,
                        widthMeasureSpec,
                        heightMeasureSpec
                    )
                    param.result = null
                    return@createBeforeHook
                }

                var availableWidth = View.MeasureSpec.getSize(widthMeasureSpec)
                if (viewGroup.callMethodAs("getQsHeaderLayout")) {
                    availableWidth /= 2
                }
                viewGroup.setIntField("availableWidth", availableWidth)

                viewGroup.getObjectFieldAs<View>("carrierSeparatorView").isVisible = false
                val leftCarrierTextView = viewGroup.getObjectFieldAs<View>("leftCarrierTextView")
                val rightCarrierTextView = viewGroup.getObjectFieldAs<View>("rightCarrierTextView")
                if (leftCarrierTextView.callMethodAs("shouldShow")) {
                    viewGroup.callMethod("setCarrierMaxWidth", leftCarrierTextView, availableWidth)
                } else {
                    viewGroup.callMethod("setCarrierMaxWidth", rightCarrierTextView, availableWidth)
                }

                // super.onMeasure
            invokeSuperMethod(
                "onMeasure", viewGroup,
                View.MeasureSpec.makeMeasureSpec(availableWidth, View.MeasureSpec.EXACTLY),
                heightMeasureSpec
            )
                viewGroup.callMethod(
                    "setMeasuredDimension",
                    availableWidth,
                    viewGroup.measuredHeight
                )

                param.result = null
            }
    }

    // 隐藏全部名称
    private fun hideCarrierText() {
        loadClass("com.android.systemui.controlcenter.shade.ControlCenterHeaderController").findMethod { name("updateCarrierAndPrivacyVisible") }.createAfterHook { param ->
                param.thisObject.getObjectFieldAs<View>("carrierLayout").visibility = View.INVISIBLE
            }
    }
}
