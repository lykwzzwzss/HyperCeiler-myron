/*
  * This file is part of HyperCeiler.

  * HyperCeiler is free software: you can redistribute it and/or modify
  * it under the terms of the GNU Affero General Public License as
  * published by the Free Software Foundation, either version 3 of the
  * License.

  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  * GNU Affero General Public License for more details.

  * You should have received a copy of the GNU Affero General Public License
  * along with this program.  If not, see <https://www.gnu.org/licenses/>.

  * Copyright (C) 2023-2026 HyperCeiler Contributions
*/
package com.sevtinge.hyperceiler.libhook.rules.systemui.lockscreen

import android.net.Uri
import com.sevtinge.hyperceiler.libhook.base.BaseHook
import io.github.lingqiqi5211.ezhooktool.core.findMethod
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.beforeHookMethod
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.getObjectFieldOrNullAs
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.setObjectField
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createBeforeHook

object HideLockscreenZenMode : BaseHook() {
    override fun init() {
        val legacyZenModeClass = findClassIfExists(
            "com.android.systemui.statusbar.notification.zen.ZenModeViewController"
        )
        if (legacyZenModeClass != null) {
            legacyZenModeClass.findMethod { filter { name.startsWith("updateVisibility") } }
                .createBeforeHook {
                    it.thisObject.setObjectField("manuallyDismissed", true)
                }
            return
        }

        // Android 17 moved the lockscreen DND indicator into a Slice row. Suppress only
        // that row by URI, leaving the device-wide Zen mode state untouched.
        val listBuilderImpl = findClassIfExists("androidx.slice.builders.impl.ListBuilderImpl") ?: return
        listBuilderImpl.beforeHookMethod("addRow") { param ->
            val row = param.args.firstOrNull() ?: return@beforeHookMethod
            val uri = row.getObjectFieldOrNullAs<Uri>("mUri") ?: return@beforeHookMethod
            if (uri == Uri.parse("content://com.android.systemui.keyguard/dnd")) {
                param.result = null
            }
        }
    }
}
