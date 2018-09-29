package com.xt.m_picker_view.address_select;

import android.content.Context;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.view.View;

import com.bigkoo.pickerview.builder.OptionsPickerBuilder;
import com.bigkoo.pickerview.listener.OnOptionsSelectListener;
import com.bigkoo.pickerview.view.OptionsPickerView;
import com.google.gson.Gson;

import org.json.JSONArray;

import java.util.ArrayList;

/**
 * Created by xuti on 2017/8/23.
 */

public abstract class AddressSelector {
    public enum AddressLevel {
        LEVEL2, LEVEL3
    }

    private AddressLevel addressLevel;

    public abstract void onAddressSelected(String[] addresses);

    public AddressSelector(AddressLevel addressLevel) {
        this.addressLevel = addressLevel;
    }

    /****************************************日期选择器相关***********************************************/
    private OptionsPickerView pvOptions;
    private ArrayList<JsonBean> options1Items = new ArrayList<>();
    private ArrayList<ArrayList<String>> options2Items = new ArrayList<>();
    private ArrayList<ArrayList<ArrayList<String>>> options3Items = new ArrayList<>();
    String province;
    String city;
    String district;

    public void showPicker(Context context, String[] addresses) {//条件选择器初始化
        if (pvOptions == null) {
            pvOptions = initOptionsPickerView(context);
        }
        int[] provinceAddressPosition = getProvinceAddressPosition(addresses);
        pvOptions.setSelectOptions(provinceAddressPosition[0], provinceAddressPosition[1], provinceAddressPosition[2]);
        pvOptions.show();
    }

    @NonNull
    private OptionsPickerView initOptionsPickerView(Context context) {
        OptionsPickerView pvOptions = new OptionsPickerBuilder(context, new OnOptionsSelectListener() {
            @Override
            public void onOptionsSelect(int options1, int options2, int options3, View v) {
                //返回的分别是三个级别的选中位置
                province = options1Items.get(options1).getPickerViewText();
                city = options2Items.get(options1).get(options2);
                district = options3Items.get(options1).get(options2).get(options3);
                onAddressSelected(new String[]{province, city, district});

//                Toast.makeText(JsonDataActivity.this,tx,Toast.LENGTH_SHORT).show();
            }
        })
                .setSubmitText("确定")//确定按钮文字
                .setCancelText("取消")//取消按钮文字
                .setTitleText("城市选择")//标题
                .setSubCalSize(18)//确定和取消文字大小
                .setTitleSize(20)//标题文字大小
                .setTitleColor(Color.BLACK)//标题文字颜色
                .setSubmitColor(Color.BLUE)//确定按钮文字颜色
                .setCancelColor(Color.BLUE)//取消按钮文字颜色
                .setTitleBgColor(0xFF333333)//标题背景颜色 Night mode
                .setBgColor(0xFF000000)//滚轮背景颜色 Night mode
                .setContentTextSize(18)//滚轮文字大小
//                .setLinkage(false)//设置是否联动，默认true
                .setLabels("省", "市", "区")//设置选择的三级单位
                .isCenterLabel(false) //是否只显示中间选中项的label文字，false则每项item全部都带有label。
                .setCyclic(false, false, false)//循环与否
                .setSelectOptions(1, 1, 1)  //设置默认选中项
                .setOutSideCancelable(false)//点击外部dismiss default true
                .isDialog(true)//是否显示为对话框样式
                .isRestoreItem(true)//切换时是否还原，设置默认选中第一项。
                .build();
        initJsonData(context);
        /*pvOptions.setPicker(options1Items);//一级选择器
        pvOptions.setPicker(options1Items, options2Items);//二级选择器*/
        switch (addressLevel) {
            case LEVEL2:
                pvOptions.setPicker(options1Items, options2Items);//二级选择器
                break;
            case LEVEL3:
                pvOptions.setPicker(options1Items, options2Items, options3Items);//三级选择器
                break;
        }
        return pvOptions;
    }

    private void initJsonData(Context context) {//解析数据

        /**
         * 注意：assets 目录下的Json文件仅供参考，实际使用可自行替换文件
         * 关键逻辑在于循环体
         *
         * */
        String JsonData = new GetJsonDataUtil().getJson(context.getApplicationContext(), "province.json");//获取assets目录下的json文件数据

        ArrayList<JsonBean> jsonBean = parseData(JsonData);//用Gson 转成实体

        /**
         * 添加省份数据
         *
         * 注意：如果是添加的JavaBean实体，则实体类需要实现 IPickerViewData 接口，
         * PickerView会通过getPickerViewText方法获取字符串显示出来。
         */
        options1Items = jsonBean;

        for (int i = 0; i < jsonBean.size(); i++) {//遍历省份
            ArrayList<String> CityList = new ArrayList<>();//该省的城市列表（第二级）
            ArrayList<ArrayList<String>> Province_AreaList = new ArrayList<>();//该省的所有地区列表（第三极）

            for (int c = 0; c < jsonBean.get(i).getCityList().size(); c++) {//遍历该省份的所有城市
                String CityName = jsonBean.get(i).getCityList().get(c).getName();
                CityList.add(CityName);//添加城市

                ArrayList<String> City_AreaList = new ArrayList<>();//该城市的所有地区列表

                //如果无地区数据，建议添加空字符串，防止数据为null 导致三个选项长度不匹配造成崩溃
                if (jsonBean.get(i).getCityList().get(c).getArea() == null
                        || jsonBean.get(i).getCityList().get(c).getArea().size() == 0) {
                    City_AreaList.add("");
                } else {

                    for (int d = 0; d < jsonBean.get(i).getCityList().get(c).getArea().size(); d++) {//该城市对应地区所有数据
                        String AreaName = jsonBean.get(i).getCityList().get(c).getArea().get(d);

                        City_AreaList.add(AreaName);//添加该城市所有地区数据
                    }
                }
                Province_AreaList.add(City_AreaList);//添加该省所有地区数据
            }

            /**
             * 添加城市数据
             */
            options2Items.add(CityList);

            /**
             * 添加地区数据
             */
            options3Items.add(Province_AreaList);
        }

//        mHandler.sendEmptyMessage(MSG_LOAD_SUCCESS);

    }

    public ArrayList<JsonBean> parseData(String result) {//Gson 解析
        ArrayList<JsonBean> detail = new ArrayList<>();
        try {
            JSONArray data = new JSONArray(result);
            Gson gson = new Gson();
            for (int i = 0; i < data.length(); i++) {
                JsonBean entity = gson.fromJson(data.optJSONObject(i).toString(), JsonBean.class);
                detail.add(entity);
            }
        } catch (Exception e) {
            e.printStackTrace();
//            mHandler.sendEmptyMessage(MSG_LOAD_FAILED);
        }
        return detail;
    }

    public int[] getProvinceAddressPosition(String[] address) {
        int[] positions = new int[3];
        if (TextUtils.isEmpty(address[0])) {
            return positions;
        }
        for (int i = 0; i < options1Items.size(); i++) {
            String province = options1Items.get(i).getName();
            if (province.contains(address[0])) {
                positions[0] = i;
                if (TextUtils.isEmpty(address[1])) {
                    return positions;
                }
                for (int j = 0; j < options2Items.get(i).size(); j++) {
                    String city = options2Items.get(i).get(j);
                    if (city.contains(address[1])) {
                        positions[1] = j;
                        if (TextUtils.isEmpty(address[2])) {
                            return positions;
                        }
                        for (int k = 0; k < options3Items.get(i).get(j).size(); k++) {
                            String district = options3Items.get(i).get(j).get(k);
                            if (district.contains(address[2])) {
                                positions[2] = k;
                            }
                        }
                    }
                }
            }
        }
        return positions;
    }

    /****************************************日期选择器相关***********************************************/
    @NonNull
    public static String[] getAddressesOfCurrent(String address) {
        String[] addresses;
        if (TextUtils.isEmpty(address)) {
            addresses = new String[]{"", "", ""};
        } else {
            String[] split = new String[2];
            if (address.contains("-")) {
                split = address.split("-");
            } else if (address.contains(" ")) {
                split = address.split(" ");
            }
            addresses = new String[]{split[0], split[1], ""};
        }
        return addresses;
    }

    public static String getAddress1(String[] address) {
        String substring1;
        String substring2;
        if (address[0].contains("省") || address[0].contains("市")) {
            substring1 = address[0].substring(0, address[0].length() - 1);
        } else {
            substring1 = address[0];
        }
        if (address[1].contains("市")) {
            substring2 = address[1].substring(0, address[1].length() - 1);
        } else {
            substring2 = address[1];
        }
        return substring1 + " " + substring2;
    }

    public static String getAddress2(String[] address) {
        return address[0] + " " + address[1] + " " + address[2];
    }

    public static String getAddress3(String[] address) {
        return address[0] + " " + address[1];
    }
}
