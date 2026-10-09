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
package com.sevtinge.hyperceiler.libhook.rules.getapps;

import android.os.Build;

import com.sevtinge.hyperceiler.common.utils.PrefsBridge;
import com.sevtinge.hyperceiler.libhook.base.BaseHook;

import io.github.lingqiqi5211.ezhooktool.xposed.common.HookParam;
import io.github.lingqiqi5211.ezhooktool.xposed.java.IMethodHook;


public class DeviceModify extends BaseHook {

    String mDevice;
    String mModel;
    String mManufacturer;
    private int mMode;

    public static int getConfiguredMode() {
        return DeviceModifyModePolicy.resolveConfiguredMode(PrefsBridge.getAll());
    }

    @Override
    public void init() {
        mMode = getConfiguredMode();
        if (mMode == 108) {
            // 15sp
            mDevice = "dijun"; // O2S
            mModel = "25042PN24C";
            mManufacturer = "Xiaomi";
        } else if (mMode == 112) {
            // 17u
            mDevice = "nezha"; // P1
            mModel = "2512BPNDAC";
            mManufacturer = "Xiaomi";
        } else if (mMode == 111) {
            // 17pm
            mDevice = "popsicle"; // P2
            mModel = "25098PN5AC";
            mManufacturer = "Xiaomi";
        } else if (mMode == 110) {
            // 17p
            mDevice = "pandora"; // Q200
            mModel = "2509FPN0BC";
            mManufacturer = "Xiaomi";
        } else if (mMode == 109) {
            // 17
            mDevice = "pudding"; // P3
            mModel = "25113PN0EC";
            mManufacturer = "Xiaomi";
        } else if (mMode == 107) {
            // 15u
            mDevice = "xuanyuan"; // O1
            mModel = "25010PN30C";
            mManufacturer = "Xiaomi";
        } else if (mMode == 106) {
            // 15p
            mDevice = "haotian"; // O2
            mModel = "2410DPN6CC";
            mManufacturer = "Xiaomi";
        } else if (mMode == 105) {
            // 15
            mDevice = "dada"; // O3
            mModel = "24129PN74C";
            mManufacturer = "Xiaomi";
        } else if (mMode == 156) {
            // civi5p
            mDevice = "luming";
            mModel = "25067PYE3C";
            mManufacturer = "Xiaomi";
        } else if (mMode == 202) {
            // flip2
            mDevice = "bixi"; // O8
            mModel = "2505APX7BC";
            mManufacturer = "Xiaomi";
        } else if (mMode == 224) {
            // f4
            mDevice = "goku";  // N18
            mModel = "24072PX77C";
            mManufacturer = "Xiaomi";
        } else if (mMode == 190) {
            // alpha
            mDevice = "avenger";
            mModel = "MIX Alpha";
            mManufacturer = "Xiaomi";
        } else if (mMode == 214) {
            // pad8
            mDevice = "yupei"; // P82
            mModel = "25097RP43C";
            mManufacturer = "Xiaomi";
        } else if (mMode == 215) {
            // pad8p
            mDevice = "piano"; // P81
            mModel = "25091RP04C";
            mManufacturer = "Xiaomi";
        } else if (mMode == 212) {
            // pad7u
            mDevice = "jinghu";
            mModel = "25032RP42C";
            mManufacturer = "Xiaomi";
        } else if (mMode == 213) {
            // pad7sp
            mDevice = "violin";
            mModel = "25053RP5CC";
            mManufacturer = "Xiaomi";
        } else if (mMode == 316) {
            // k90pm
            mDevice = "myron";
            mModel = "25102RKBEC";
            mManufacturer = "Redmi";
        } else if (mMode == 315) {
            // k90
            mDevice = "annibale";
            mModel = "2510DRK44C";
            mManufacturer = "Redmi";
        } else if (mMode == 314) {
            // k80u
            mDevice = "dali";
            mModel = "25060RK16C";
            mManufacturer = "Redmi";
        } else if (mMode == 313) {
            // k80pc
            mDevice = "miro";
            mModel = "24127RK2CC";
            mManufacturer = "Redmi";
        } else if (mMode == 380) {
            // k Pad
            mDevice = "turner";
            mModel = "25079RPDCC";
            mManufacturer = "Redmi";
        } else if (mMode == 335) {
            // t4p
            mDevice = "onyx";
            mModel = "25053RT47C";
            mManufacturer = "Redmi";
        } else if (mMode == 355) {
            // n15p+
            mDevice = "flourite";
            mModel = "2510ERA8BC"; // P16U
            mManufacturer = "Redmi";
        } else if (mMode == 403) {
            // 14c
            mDevice = "lake";
            mModel = "2409BRN2CC";
            mManufacturer = "Redmi";
        } else if (mMode == 1) {
            // customization
            mDevice = nonBlank(PrefsBridge.getString("market_device_modify_device", ""), Build.DEVICE);
            mModel = nonBlank(PrefsBridge.getString("market_device_modify_model", ""), Build.MODEL);
            mManufacturer = nonBlank(PrefsBridge.getString("market_device_modify_manufacturer", ""), Build.MANUFACTURER);
        } else {
            return;
        }
        findAndHookConstructor("com.xiaomi.market.MarketApp", new IMethodHook() {
            @Override
            public void before(HookParam param) {
                setStaticObjectField(Build.class, "DEVICE", mDevice);
                setStaticObjectField(Build.class, "MODEL", mModel);
                setStaticObjectField(Build.class, "MANUFACTURER", mManufacturer);
            }
        });
    }
    private static String nonBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

}


