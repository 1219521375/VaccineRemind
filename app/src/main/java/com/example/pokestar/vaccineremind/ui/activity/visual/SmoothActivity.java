package com.example.pokestar.vaccineremind.ui.activity.visual;

import android.app.Activity;
import android.graphics.BitmapFactory;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.amap.api.maps.model.MyLocationStyle;
import com.example.pokestar.vaccineremind.R;


import com.amap.api.maps.AMap;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.MapView;
import com.amap.api.maps.model.BitmapDescriptor;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.LatLngBounds;
import com.amap.api.maps.model.Marker;
import com.amap.api.maps.model.MarkerOptions;
import com.amap.api.maps.model.Polyline;
import com.amap.api.maps.model.PolylineOptions;
import com.amap.api.maps.utils.SpatialRelationUtil;
import com.amap.api.maps.utils.overlay.MovingPointOverlay;
import com.example.pokestar.vaccineremind.utils.ToastUtil;

import android.content.Intent;
import android.graphics.Color;

import android.util.Pair;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class SmoothActivity extends Activity {

    int choose;
    private MapView mMapView;
    private AMap mAMap;
    private Polyline mPolyline;
    private MovingPointOverlay smoothMarker;
    private Marker marker;
    private MyLocationStyle myLocationStyle;
    private TextView  text;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_smooth);

        mMapView = (MapView) findViewById(R.id.map);
        mMapView.onCreate(savedInstanceState);

        init();

    }




    /*写一个新的线程每隔一秒发送一次消息,这样做会和系统时间相差1秒
    //主活动
     mainTv = findViewById(R.id.textview);
        new TimeThread().start();//启动线程


    public class TimeThread extends Thread{
        @Override
        public void run() {
            super.run();
            do{
                try {
                    Thread.sleep(1000);
                    Message msg = new Message();
                    msg.what = 1;
                    handler.sendMessage(msg);

                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }while (true);

        }
    }
    private Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what){
                case 1:
                    mainTv.setText(new SimpleDateFormat("HH:mm:ss").format(new Date(System.currentTimeMillis())));
                    break;
            }
            return false;
        }
    });
*/

    /**
     * 初始化AMap对象
     */
    private void init() {
        if (mAMap == null) {
            mAMap = mMapView.getMap();
            //进行定位
            setUpMap();
        }
    }

    private void setUpMap() {

        // 如果要设置定位的默认状态，可以在此处进行设置
        myLocationStyle = new MyLocationStyle();
        mAMap.setMyLocationStyle(myLocationStyle);

        mAMap.getUiSettings().setMyLocationButtonEnabled(true);// 设置默认定位按钮是否显示
        mAMap.setMyLocationEnabled(true);// 设置为true表示显示定位层并可触发定位，false表示隐藏定位层并不可触发定位，默认是false
        mAMap.setMyLocationStyle(myLocationStyle.myLocationType(MyLocationStyle.LOCATION_TYPE_LOCATE));
    }

    /**
     * 方法必须重写
     */
    @Override
    protected void onResume() {
        super.onResume();
        mMapView.onResume();

    }

    /**
     * 方法必须重写
     */
    @Override
    protected void onPause() {
        super.onPause();
        mMapView.onPause();

    }

    /**
     * 方法必须重写
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mMapView.onSaveInstanceState(outState);
    }

    /**
     * 方法必须重写
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();

        // 销毁平滑移动marker
        if(smoothMarker != null) {
            smoothMarker.setMoveListener(null);
            smoothMarker.destroy();
        }

        mMapView.onDestroy();



    }
/*
    public void setLine(View view) {
        addPolylineInPlayGround();
    }
*/
    /**
     * 开始移动
     */
    public void one_state(View view)
    {
        choose = 1;
        addPolylineInPlayGround();
        startMove(view);
    }
    public void two_state(View view)
    {
        Intent intent1 = new Intent(SmoothActivity.this,GISMapActivity.class);
        startActivity(intent1);
    }
    public void three_state(View view)
    {
        choose = 3;
        addPolylineInPlayGround();
        startMove(view);
    }
    public void startMove(View view) {


        if (mPolyline == null) {
            ToastUtil.showShort(this, "请先设置路线");
            return;
        }

        // 读取轨迹点
        List<LatLng> points = readLatLngs();
        // 构建 轨迹的显示区域
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        builder.include(points.get(0));
        builder.include(points.get(points.size() - 2));

        mAMap.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 50));



        // 实例 MovingPointOverlay 对象
        if(smoothMarker == null) {
            // 设置 平滑移动的 图标
            marker = mAMap.addMarker(new MarkerOptions().icon(BitmapDescriptorFactory.fromResource(R.drawable.icon_car)).anchor(0.5f,0.5f));
            smoothMarker = new MovingPointOverlay(mAMap, marker);
        }

        // 取轨迹点的第一个点 作为 平滑移动的启动
        LatLng drivePoint = points.get(0);
        Pair<Integer, LatLng> pair = SpatialRelationUtil.calShortestDistancePoint(points, drivePoint);
        points.set(pair.first, drivePoint);
        List<LatLng> subList = points.subList(pair.first, points.size());

        // 设置轨迹点
        smoothMarker.setPoints(subList);
        // 设置平滑移动的总时间  单位  秒
        smoothMarker.setTotalDuration(30);

        // 设置  自定义的InfoWindow 适配器
        mAMap.setInfoWindowAdapter(infoWindowAdapter);
        // 显示 infowindow
        marker.showInfoWindow();

        // 设置移动的监听事件  返回 距终点的距离  单位 米
        smoothMarker.setMoveListener(new MovingPointOverlay.MoveListener() {
            @Override
            public void move(final double distance) {

                try {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (infoWindowLayout != null && title != null) {
                                if(choose == 1)
                                {
                                    title.setText("距离上海虹桥国际机场还有：" + (int) distance + "米"+"\n"+"疫苗当前温度  ： 12℃" + "\n当前环境 ： 氮气箱");
                                }
                                else if(choose == 3)
                                {
                                    title.setText("距离太原疾控中心还有： " + (int) distance + "米"+"\n"+"疫苗当前温度   ：  12℃" + "\n当前环境 ： 氮气箱");
                                }
                            }
                        }
                    });
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }
        });

        // 开始移动
        smoothMarker.startSmoothMove();

    }

    /**
     *  个性化定制的信息窗口视图的类
     *  如果要定制化渲染这个信息窗口，需要重载getInfoWindow(Marker)方法。
     *  如果只是需要替换信息窗口的内容，则需要重载getInfoContents(Marker)方法。
     */
    AMap.InfoWindowAdapter infoWindowAdapter = new AMap.InfoWindowAdapter(){

        // 个性化Marker的InfoWindow 视图
        // 如果这个方法返回null，则将会使用默认的信息窗口风格，内容将会调用getInfoContents(Marker)方法获取
        @Override
        public View getInfoWindow(Marker marker) {

            return getInfoWindowView(marker);
        }

        // 这个方法只有在getInfoWindow(Marker)返回null 时才会被调用
        // 定制化的view 做这个信息窗口的内容，如果返回null 将以默认内容渲染
        @Override
        public View getInfoContents(Marker marker) {

            return getInfoWindowView(marker);
        }
    };

    LinearLayout infoWindowLayout;
    TextView title;
    TextView snippet;

    /**
     * 自定义View并且绑定数据方法
     * @param marker 点击的Marker对象
     * @return  返回自定义窗口的视图
     */
    private View getInfoWindowView(Marker marker) {
        if (infoWindowLayout == null) {
            infoWindowLayout = new LinearLayout(this);
            infoWindowLayout.setOrientation(LinearLayout.VERTICAL);
            title = new TextView(this);
            snippet = new TextView(this);
            title.setText("距离距离展示");
            title.setTextColor(Color.BLACK);
            snippet.setTextColor(Color.BLACK);
            infoWindowLayout.setBackgroundResource(R.drawable.infowindow_bg);

            infoWindowLayout.addView(title);
            infoWindowLayout.addView(snippet);
        }

        return infoWindowLayout;
    }

    /**
     * 添加轨迹线
     */
    private void addPolylineInPlayGround() {
        List<LatLng> list = readLatLngs();
        List<Integer> colorList = new ArrayList<Integer>();
        List<BitmapDescriptor> bitmapDescriptors = new ArrayList<BitmapDescriptor>();

        int[] colors = new int[]{Color.argb(255, 0, 255, 0),Color.argb(255, 255, 255, 0),Color.argb(255, 255, 0, 0)};

        //用一个数组来存放纹理
        List<BitmapDescriptor> textureList = new ArrayList<BitmapDescriptor>();
        textureList.add(BitmapDescriptorFactory.fromResource(R.drawable.custtexture));

        List<Integer> texIndexList = new ArrayList<Integer>();
        texIndexList.add(0);//对应上面的第0个纹理
        texIndexList.add(1);
        texIndexList.add(2);

        Random random = new Random();
        for (int i = 0; i < list.size(); i++) {
            colorList.add(colors[random.nextInt(3)]);
            bitmapDescriptors.add(textureList.get(0));

        }

        mPolyline = mAMap.addPolyline(new PolylineOptions().setCustomTexture(BitmapDescriptorFactory.fromResource(R.drawable.custtexture)) //setCustomTextureList(bitmapDescriptors)
//				.setCustomTextureIndex(texIndexList)
                .addAll(list)
                .useGradient(true)
                .width(18));

        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        builder.include(list.get(0));
        builder.include(list.get(list.size() - 2));

        mAMap.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 100));
    }

    /**
     * 读取坐标点
     * @return
     */

    private List<LatLng> readLatLngs() {
        List<LatLng> points = new ArrayList<LatLng>();
        text = findViewById(R.id.text);
        //    text.setText("一切正常"+"\n"+"温度   ：  12℃"+"\n"+"所处环境   ： 氮气箱"+"\n");
        if(choose == 1 )
        {
            for (int i = 0; i < coords3.length; i += 2) {
                LatLng latLng = new LatLng(30.9562380000,121.4819110000);
                Marker marker1 = mAMap.addMarker(new MarkerOptions().position(latLng));
                BitmapDescriptor bit  = BitmapDescriptorFactory.fromBitmap(BitmapFactory.decodeResource(getResources(),R.drawable.shang));
                marker1.setIcon(bit);
                points.add(new LatLng(coords3[i], coords3[i+1]));
            }
        }
        else if(choose == 3)
        {
            for (int i = 0; i < coords2.length; i += 2) {
                LatLng latLng = new LatLng(37.8463250000,112.5514100000);
                Marker marker2 = mAMap.addMarker(new MarkerOptions().position(latLng));
                BitmapDescriptor bit  = BitmapDescriptorFactory.fromBitmap(BitmapFactory.decodeResource(getResources(),R.drawable.tai));
                marker2.setIcon(bit);
                points.add(new LatLng(coords2[i], coords2[i+1]));
            }
        }
        return points;
    }

    /**
     * 坐标点数组数据
     */
    private double[] coords = { 116.3499049793749, 39.97617053371078,
            116.34978804908442, 39.97619854213431, 116.349674596623,
            39.97623045687959, 116.34955525200917, 39.97626931100656,
            116.34943728748914, 39.976285626595036, 116.34930864705592,
            39.97628129172198, 116.34918981582413, 39.976260803938594,
            116.34906721558868, 39.97623535890678, 116.34895185151584,
            39.976214717128855, 116.34886935936889, 39.976280148755315,
            116.34873954611332, 39.97628182112874, 116.34860763527448,
            39.97626038855863, 116.3484658907622, 39.976306080391836,
            116.34834585430347, 39.976358252119745, 116.34831166130878,
            39.97645709321835, 116.34827643560175, 39.97655231226543,
            116.34824186261169, 39.976658372925556, 116.34825080406188,
            39.9767570732376, 116.34825631960626, 39.976869087779995,
            116.34822111635201, 39.97698451764595, 116.34822901510276,
            39.977079745909876, 116.34822234337618, 39.97718701787645,
            116.34821627457707, 39.97730766147824, 116.34820593515043,
            39.977417746816776, 116.34821013897107, 39.97753930933358
            ,116.34821304891533, 39.977652209132174, 116.34820923399242,
            39.977764016531076, 116.3482045955917, 39.97786190186833,
            116.34822159449203, 39.977958856930286, 116.3482256370537,
            39.97807288885813, 116.3482098441266, 39.978170063673524,
            116.34819564465377, 39.978266951404066, 116.34820541974412,
            39.978380693859116, 116.34819672351216, 39.97848741209275,
            116.34816588867105, 39.978593409607825, 116.34818489339459,
            39.97870216883567, 116.34818473446943, 39.978797222300166,
            116.34817728972234, 39.978893492422685, 116.34816491505472,
            39.978997133775266, 116.34815408537773, 39.97911413849568,
            116.34812908154862, 39.97920553614499, 116.34809495907906,
            39.979308267469264, 116.34805113358091, 39.97939658036473,
            116.3480310509613, 39.979491697188685, 116.3480082124968,
            39.979588529006875, 116.34799530586834, 39.979685789111635,
            116.34798818413954, 39.979801430587926, 116.3479996420353,
            39.97990758587515, 116.34798697544538, 39.980000796262615,
            116.3479912988137, 39.980116318796085, 116.34799204219203,
            39.98021407403913, 116.34798535084123, 39.980325006125696,
            116.34797702460183, 39.98042511477518, 116.34796288754136,
            39.98054129336908, 116.34797509821901, 39.980656820423505,
            116.34793922017285, 39.98074576792626, 116.34792586413015,
            39.98085620772756, 116.3478962642899, 39.98098214824056,
            116.34782449883967, 39.98108306010269, 116.34774758827285,
            39.98115277119176, 116.34761476652932, 39.98115430642997,
            116.34749135408349, 39.98114590845294, 116.34734772765582,
            39.98114337322547, 116.34722082902628, 39.98115066909245,
            116.34708205250223, 39.98114532232906, 116.346963237696,
            39.98112245161927, 116.34681500222743, 39.981136637759604,
            116.34669622104072, 39.981146248090866, 116.34658043260109,
            39.98112495260716, 116.34643721418927, 39.9811107163792,
            116.34631638374302, 39.981085081075676, 116.34614782996252,
            39.98108046779486, 116.3460256053666, 39.981049089345206,
            116.34588814050122, 39.98104839362087, 116.34575119741586,
            39.9810544889668, 116.34562885420186, 39.981040940565734,
            116.34549232235582, 39.98105271658809, 116.34537348820508,
            39.981052294975264, 116.3453513775533, 39.980956549928244
    };
    private double[] coords1 = {
            43.7993600000,125.2542100000,
            43.7990110000,125.2538110000,
            43.7989950000,125.2509150000,
            43.7989330000,125.2467300000
    };
    //太原
    private double[] coords2 = {
            37.7547020000,112.6335800000,
            37.7546760000,112.6338260000,
            37.7547650000,112.6339660000,
            37.7549860000,112.6359830000,
            37.7549650000,112.6362620000,
            37.7549350000,112.6365570000,
            37.7549520000,112.6366700000,
            37.7550370000,112.6368730000,
            37.7551640000,112.6370130000,
            37.7553340000,112.6371100000,
            37.7555380000,112.6371420000,
            37.7557290000,112.6370770000,
            37.7559280000,112.6368730000,
            37.7561950000,112.6364280000,
            37.7569970000,112.6349910000,
            37.7571580000,112.6348510000,
            37.7576370000,112.6346100000,
            37.7580020000,112.6345400000,
            37.7641180000,112.6345670000,
            37.7650420000,112.6347490000,
            37.7664580000,112.6354250000,
            37.7668570000,112.6355110000,
            37.7675190000,112.6354890000,
            37.7681210000,112.6352960000,
            37.7683630000,112.6352910000,
            37.7692960000,112.6348400000,
            37.7696440000,112.6347540000,
            37.7706780000,112.6347330000,
            37.7710260000,112.6345830000,
            37.7713140000,112.6343140000,
            37.7716530000,112.6338960000,
            37.7718310000,112.6331770000,
            37.7717970000,112.6320720000,
            37.7718140000,112.6316960000,
            37.7720090000,112.6306770000,
            37.7719670000,112.6305160000,
            37.7742760000,112.6260770000,
            37.7762440000,112.6216140000,
            37.7778040000,112.6194680000,
            37.7930660000,112.6017010000,
            37.7952370000,112.6000700000,
            37.7949660000,112.5988690000,
            37.7934060000,112.5969800000,
            37.7940160000,112.5456540000,
            37.7956440000,112.5435940000,
            37.7996460000,112.5431640000,
            37.8168700000,112.5446240000,
            37.8248700000,112.5458250000,
            37.8319210000,112.5449670000,
            37.8385300000,112.5418350000,
            37.8403600000,112.5412980000,
            37.8404620000,112.5413680000,
            37.8440210000,112.5456490000,
            37.8451650000,112.5472530000,
            37.8468250000,112.5495700000,
            37.8470710000,112.5498920000,
            37.8471600000,112.5501390000,
            37.8475200000,112.5506540000,
            37.8477000000,112.5511820000,
            37.8477040000,112.5519710000,
            37.8475560000,112.5519660000,
            37.8474120000,112.5519660000,
            37.8473310000,112.5519760000,
            37.8472420000,112.5519760000,
            37.8471280000,112.5519660000,
            37.8469370000,112.5519760000,
            37.8467590000,112.5519760000,
            37.8465350000,112.5519870000,
            37.8463400000,112.5519920000,
            37.8463380000,112.5518560000,
            37.8463250000,112.5514100000
    };

    //上海
    private double[] coords3 = {
            30.9562380000,121.4819110000,
            30.9562430000,121.4820340000,
            30.9560410000,121.4820770000,
            30.9559740000,121.4815190000,
            30.9558960000,121.4807630000,
            30.9558640000,121.4801350000,
            30.9557490000,121.4792130000,
            30.9556980000,121.4786340000,
            30.9556380000,121.4778240000,
            30.9555000000,121.4770030000,
            30.9553480000,121.4761180000,
            30.9552330000,121.4755060000,
            30.9551460000,121.4748250000,
            30.9557300000,121.4746260000,
            30.9574550000,121.4740740000,
            30.9625060000,121.4724170000,
            30.9664800000,121.4711940000,
            30.9707490000,121.4698850000,
            30.9739360000,121.4688390000,
            30.9738760000,121.4672080000,
            30.9737520000,121.4663440000,
            30.9735630000,121.4653950000,
            30.9732780000,121.4642310000,
            30.9728960000,121.4643220000,
            30.9732500000,121.4641450000,
            30.9726520000,121.4642580000,
            30.9721230000,121.4642070000,
            30.9719250000,121.4641430000,
            30.9713000000,121.4637030000,
            30.9709920000,121.4635040000,
            30.9705360000,121.4627640000,
            30.9701540000,121.4620830000,
            30.9699840000,121.4612460000,
            30.9699700000,121.4607470000,
            30.9701270000,121.4601460000,
            30.9704760000,121.4596260000,
            30.9711020000,121.4591540000,
            30.9716030000,121.4589020000,
            30.9716670000,121.4588460000,
            30.9745460000,121.4578590000,
            30.9781520000,121.4567860000,
            30.9816840000,121.4554990000,
            30.9856580000,121.4535250000,
            30.9897050000,121.4515500000,
            30.9955910000,121.4477740000,
            30.9976840000,121.4467380000,
            30.9976840000,121.4467380000,
            31.0019970000,121.4443590000,
            31.0024660000,121.4442060000,
            31.0039100000,121.4433020000,
            31.0059970000,121.4421860000,
            31.0091470000,121.4404560000,
            31.0121030000,121.4390560000,
            31.0193850000,121.4350220000,
            31.0217380000,121.4343350000,
            31.0237980000,121.4332200000,
            31.0285790000,121.4314170000,
            31.0339480000,121.4289280000,
            31.0351250000,121.4281560000,
            31.0418170000,121.4253230000,
            31.0466700000,121.4231770000,
            31.0509970000,121.4209380000,
            31.0546000000,121.4196930000,
            31.0604090000,121.4170330000,
            31.0621730000,121.4158360000,
            31.0630740000,121.4156540000,
            31.0638180000,121.4155630000,
            31.0642090000,121.4154020000,
            31.0652890000,121.4146560000,
            31.0658310000,121.4141840000,
            31.0677380000,121.4132990000,
            31.0696040000,121.4124300000,
            31.0737160000,121.4105630000,
            31.0754070000,121.4097800000,
            31.0810490000,121.4071940000,
            31.0886750000,121.4037500000,
            31.0972280000,121.4005900000,
            31.1011780000,121.3990350000,
            31.1032360000,121.3980260000,
            31.1059000000,121.3965030000,
            31.1124680000,121.3922650000,
            31.1172400000,121.3891910000,
            31.1182830000,121.3886440000,
            31.1196010000,121.3882440000,
            31.1212450000,121.3876490000,
            31.1220850000,121.3870270000,
            31.1225670000,121.3865490000,
            31.1252950000,121.3847360000,
            31.1257170000,121.3843820000,
            31.1491890000,121.3711640000,
            31.1528980000,121.3690610000,
            31.1569750000,121.3674090000,
            31.1681200000,121.3630530000,
            31.1769260000,121.3595450000,
            31.1793030000,121.3589220000,
            31.1816020000,121.3586270000,
            31.1827400000,121.3592390000,
            31.1835840000,121.3602150000,
            31.1844470000,121.3606550000,
            31.1849200000,121.3606120000,
            31.1854800000,121.3601500000,
            31.1864480000,121.3580910000,
            31.1870770000,121.3569810000,
            31.1878800000,121.3561810000,
            31.1896330000,121.3546420000,
            31.1910560000,121.3533760000,
            31.1921070000,121.3526300000,
            31.1930340000,121.3516860000,
            31.1939150000,121.3503080000,
            31.1939680000,121.3502790000,
            31.1947670000,121.3475980000,
    };
    //山西农业大学
    private double[] coords4 = {
            37.4240520000,112.5833800000,
            37.4240010000,112.5842280000,
            37.4237960000,112.5855370000,
            37.4233620000,112.5855150000,
            37.4230890000,112.5854830000,
            37.4230210000,112.5862340000,
            37.4228000000,112.5877790000,
            37.4214960000,112.5875640000,
            37.4214280000,112.5881970000,
            37.4213080000,112.5898600000,
            37.4218540000,112.5899840000,
            37.4230040000,112.5901230000,
            37.4233450000,112.5901450000,
            37.4234640000,112.5902360000,
            37.4238350000,112.5902730000,
            37.4245160000,112.5903750000,
            37.4244780000,112.5906760000,
            37.4244400000,112.5909710000,
            37.4248020000,112.5909760000,
            37.4248360000,112.5919100000,
    };
}
