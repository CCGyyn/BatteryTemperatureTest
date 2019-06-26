package com.miki.batterytemperaturetest;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.UserHandle;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.FileWriter;
import java.io.IOException;

/**
 * @author cai_gp
 */
@RequiresApi(api = Build.VERSION_CODES.L)
public class BatteryTemperatureTest extends AppCompatActivity implements View.OnClickListener{

    private String TAG = BatteryTemperatureTest.class.getSimpleName();
    private TextView test;
    private Button button1;
    private Context mContext;
    private int mCriticalBatteryLevel;
    private int mLowBatteryWarningLevel;
    private int mLowBatteryCloseWarningLevel;
    private int mShutdownBatteryTemperature;

    private static final String WAKE_PATH = "/sys/devices/platform/battery/Enable_Write_Temp";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_battery_temperature_test);
        mContext = this;
        test = (TextView) findViewById(R.id.test);
        button1 = (Button) findViewById(R.id.button1);
        String apkRoot="chmod 777 "+getPackageCodePath();
        RootCommand(apkRoot);
        // 低电量临界值，（在这个类里只是用来写日志）
        mCriticalBatteryLevel = mContext.getResources().getInteger(
                com.android.internal.R.integer.config_criticalBatteryWarningLevel);
        // 低电量告警值，值15，下面会根据这个变量发送低电量的广播Intent.ACTION_BATTERY_LOW（这个跟系统低电量提醒没关系，只是发出去了
        mLowBatteryWarningLevel = mContext.getResources().getInteger(
                com.android.internal.R.integer.config_lowBatteryWarningLevel);
        // 电量告警取消值，值20 ， 就是手机电量大于等于20的话发送Intent.ACTION_BATTERY_OKAY
        mLowBatteryCloseWarningLevel = mLowBatteryWarningLevel + mContext.getResources().getInteger(
                com.android.internal.R.integer.config_lowBatteryCloseWarningBump);
        // 温度过高，超过这个值就发送广播，跳转到将要关机提醒
        mShutdownBatteryTemperature = mContext.getResources().getInteger(
                com.android.internal.R.integer.config_shutdownBatteryTemperature);

        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        BatteryReceiver mReceiver = new BatteryReceiver();
        registerReceiver(mReceiver, intentFilter);
        button1.setOnClickListener(this);
        try {
            BufferedWriter bufWriter = null;
            bufWriter = new BufferedWriter(new FileWriter(WAKE_PATH));
            bufWriter.write("1");  // 写操作
            bufWriter.close();
            Toast.makeText(getApplicationContext(),
                    "功能已激活",Toast.LENGTH_SHORT).show();
            Log.d(TAG,"功能已激活 angle ");
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG,"can't write the " + WAKE_PATH);
        }
    }

    @Override
    public synchronized ComponentName startForegroundServiceAsUser(Intent service, UserHandle user) {
        return null;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.button1:{
//                String cmd = "echo 1000 > /sys/class/power_supply/battery/debug_jeita";
                // 设置的温度
                String template = "60";
                String cmd1 = "echo 1 > /sys/devices/platform/battery/Enable_Write_Temp";
                String cmd2 = "echo " + template + " > /sys/devices/platform/battery/Battery_Temperature";
                checkDeviceRoot();
                String cmd = "am broadcast -a \"android.intent.action.BATTERY_CHANGED\" --ei temperature 1990";
                try{
                    myLogD("开始adb shell");
                    Process p = Runtime.getRuntime().exec("system/xbin/su");
                    DataOutputStream dos = new DataOutputStream(p.getOutputStream());
                    dos.writeBytes(cmd + "\n");
                    dos.writeBytes("exit\n");
                    dos.flush();
                    Process su;
//                    Runtime.getRuntime().exec(new String[]{"/system/bin/su","-c", cmd});
                    myLogD("结束adb shell");

                }catch (java.io.IOException e) {
                    myLogD("出错了");
                    myLogD(e.toString());
                    e.printStackTrace();
                }

                break;
            }
            default:
                break;
        }
    }

    /**
     * 判断机器Android是否已经root，即是否获取root权限
     */
    public static boolean checkDeviceRoot() {
        boolean resualt = false;
        // 通过执行测试命令来检测
        int ret = execRootCmdSilent("echo test");
        if (ret != -1) {
            Log.i("checkDeviceRoot", "this Device have root!");
            resualt = true;
        } else {
            resualt = false;
            Log.i("checkDeviceRoot", "this Device not root!");
        }
        return resualt;
    }

    /**
     * 执行命令但不关注结果输出
     */
    public static int execRootCmdSilent(String cmd) {
        int result = -1;
        DataOutputStream dos = null;
        try {
            Process p = Runtime.getRuntime().exec("system/xbin/su");
            dos = new DataOutputStream(p.getOutputStream());
            dos.writeBytes(cmd + "\n");
            dos.flush();
            dos.writeBytes("exit\n");
            dos.flush();
            p.waitFor();
            result = p.exitValue();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (dos != null) {
                try {
                    dos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return result;
    }

    class BatteryReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            int temperature = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1);
            test.setText(String.valueOf(temperature));
            if(isOrderedBroadcast()) {
                myLogD("有序广播");
            } else {
                myLogD("无序广播");
            }
        }
    }

    private void myLogD(String msg) {
        Log.d(TAG, msg);
    }

    public static boolean RootCommand(String command)
    {
        Process process = null;
        DataOutputStream os = null;
        try
        {
            process = Runtime.getRuntime().exec("su");
            os = new DataOutputStream(process.getOutputStream());
            os.writeBytes(command + "\n");
            os.writeBytes("exit\n");
            os.flush();
            process.waitFor();
        } catch (Exception e)
        {
            Log.d("*** DEBUG ***", "ROOT REE" + e.getMessage());
            return false;
        } finally
        {
            try
            {
                if (os != null)
                {
                    os.close();
                }
                process.destroy();
            } catch (Exception e)
            {
            }
        }
        Log.d("*** DEBUG ***", "Root SUC ");
        return true;
    }
}
