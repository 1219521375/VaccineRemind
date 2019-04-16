package com.example.pokestar.vaccineremind.ui.activity.visual;

import android.content.res.AssetManager;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.esri.arcgisruntime.geometry.Point;
import com.esri.arcgisruntime.geometry.PointCollection;
import com.esri.arcgisruntime.geometry.SpatialReferences;
import com.esri.arcgisruntime.mapping.ArcGISScene;
import com.esri.arcgisruntime.mapping.ArcGISTiledElevationSource;
import com.esri.arcgisruntime.mapping.Basemap;
import com.esri.arcgisruntime.mapping.Surface;
import com.esri.arcgisruntime.mapping.view.Camera;
import com.esri.arcgisruntime.mapping.view.GlobeCameraController;
import com.esri.arcgisruntime.mapping.view.Graphic;
import com.esri.arcgisruntime.mapping.view.GraphicsOverlay;
import com.esri.arcgisruntime.mapping.view.LayerSceneProperties;
import com.esri.arcgisruntime.mapping.view.OrbitGeoElementCameraController;
import com.esri.arcgisruntime.mapping.view.SceneView;
import com.esri.arcgisruntime.symbology.ModelSceneSymbol;
import com.esri.arcgisruntime.symbology.Renderer;
import com.esri.arcgisruntime.symbology.SimpleRenderer;
import com.example.pokestar.vaccineremind.R;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class GISMapActivity extends AppCompatActivity {

    private static final String TAG = GISMapActivity.class.getSimpleName();

    private List<Map<String, Object>> mMissionData;
    private Timer mTimer;
    private int mKeyFrame;

    private TextView mCurrAltitude;
   // private TextView mCurrHeading;
   // private TextView mCurrPitch;
   // private TextView mCurrRoll;
    private Spinner mMissionSelector;


    private SceneView mSceneView;
    private OrbitGeoElementCameraController mOrbitCameraController;
    private Graphic mPlane3D;
    private GraphicsOverlay mSceneOverlay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gismap);

        //预加载模型资源至缓存
        copyFileFromAssetsToCache(getString(R.string.bristol_model));
        copyFileFromAssetsToCache(getString(R.string.bristol_skin));

        /** 3D地图 */
        mSceneView = findViewById(R.id.sceneView);
        Basemap basemap = new Basemap("http://www.arcgis.com/home/webmap/viewer.html?webmap=3de6c36223674114b105949caa1289e8");
        ArcGISScene scene = new ArcGISScene();
        scene.setBasemap(basemap);
        mSceneView.setScene(scene);

        // create a camera and set it as the viewpoint for when the scene loads
        Camera camera = new Camera(38.459291, -109.937576, 16000, 60.0, 0.0, 0.0);
        mSceneView.setViewpointCamera(camera);

        // add elevation data 添加高程数据
        Surface surface = new Surface();
        surface.getElevationSources().add(new ArcGISTiledElevationSource(getString(R.string.world_elevation_service_url)));
        scene.setBaseSurface(surface);

        //  为场景创建图形叠加层
        mSceneOverlay = new GraphicsOverlay();
        mSceneOverlay.getSceneProperties().setSurfacePlacement(LayerSceneProperties.SurfacePlacement.ABSOLUTE);
        mSceneView.getGraphicsOverlays().add(mSceneOverlay);

        //创建渲染器以处理更新飞机的方向
        SimpleRenderer renderer3D = new SimpleRenderer();
        Renderer.SceneProperties renderProperties = renderer3D.getSceneProperties();
        renderProperties.setHeadingExpression("[HEADING]");
        renderProperties.setPitchExpression("[PITCH]");
        renderProperties.setRollExpression("[ROLL]");
        mSceneOverlay.setRenderer(renderer3D);



        //当飞机模型完成加载后，创建一个轨道摄像机控制器来跟随飞机
        loadModel().addDoneLoadingListener(() -> {
            mOrbitCameraController = new OrbitGeoElementCameraController(mPlane3D,60);
            mOrbitCameraController.setCameraPitchOffset(75.0);
            mOrbitCameraController.setCameraHeadingOffset(50);
            mOrbitCameraController.setCameraDistance(1200000);
            mSceneView.setCameraController(mOrbitCameraController);
        });

        //获取对UI元素的引用和连接
        createUiElements();
    }

    /**
     * Setup the app's UI elements.
     */
    private void createUiElements() {
        // get UI elements
        mMissionSelector = findViewById(R.id.missionSelectorSpinner);
        mMissionSelector.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> adapterView, View view, int position, long l) {
                changeMission(mMissionSelector.getSelectedItem().toString());
            }

            @Override public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
        mMissionSelector.setSelection(2);


        // get references to HUD text views
        mCurrAltitude = findViewById(R.id.currAltitudeTextView);
        //mCurrHeading = findViewById(R.id.currHeadingTextView);
        //mCurrPitch = findViewById(R.id.currPitchTextView);
        //mCurrRoll = findViewById(R.id.currRollTextView);

    }

    /** 从缓存加载飞机模型，用于构建模型场景符号并将其添加到场景的图形叠加层。
     * Load the plane model from the cache, use to construct a Model Scene Symbol and add it to the scene's graphic overlay.
     */
    private ModelSceneSymbol loadModel() {
        // create a graphic with a ModelSceneSymbol of a plane to add to the scene
        // 从dae 文件中加载飞机模型
        String pathToModel = getCacheDir() + File.separator + getString(R.string.bristol_model);
        //scale 调整模型大小
        ModelSceneSymbol plane3DSymbol = new ModelSceneSymbol(pathToModel, 15000.0);
        plane3DSymbol.loadAsync();
        mPlane3D = new Graphic(new Point(0, 0, 0, SpatialReferences.getWgs84()), plane3DSymbol);
        mSceneOverlay.getGraphics().add(mPlane3D);
        return plane3DSymbol;
    }

    /**
     * Change the mission data and reset the animation.
     * 改变任务数据，重设动画
     * @param mission name of .csv file containing mission data
     */
    private void changeMission(String mission) {

        stopAnimation();

        // clear previous mission data
        mMissionData = new ArrayList<>();

        // get mission data
        mMissionData = getMissionData(mission);

        // draw mission route on mini map
        PointCollection points = new PointCollection(SpatialReferences.getWgs84());
        for (Map<String, Object> ordinates : mMissionData) {
            points.add((Point) ordinates.get("POSITION"));
        }

        startAnimation();

    }

    /**
     * Loads the mission data from a .csv file into memory.
     *将任务数据从.csv文件加载到内存中。
     * @param mission name of the .csv file containing the mission data
     * @return ordered list of mapped key value pairs representing coordinates and rotation parameters for each step of
     * the mission
     */
    private List<Map<String, Object>> getMissionData(String mission) {
        List<Map<String, Object>> missionList = new ArrayList<>();

        // open a file reader to the mission file that automatically closes after read
        try (BufferedReader missionFile = new BufferedReader(new InputStreamReader(getAssets().open(mission)))) {
            String line;
            while ((line = missionFile.readLine()) != null) {
                String[] l = line.split(",");
                Map<String, Object> ordinates = new HashMap<>();
                /* 经纬度 海拔 */
                ordinates.put("POSITION",
                        new Point(Float.valueOf(l[0]), Float.valueOf(l[1]), Float.valueOf(l[2]), SpatialReferences.getWgs84()));
                /* 头指向角度 */
                ordinates.put("HEADING", Float.valueOf(l[3]));
                /*俯仰角 */
                ordinates.put("PITCH", Float.valueOf(l[4]));
                /* 横摆角 */
                ordinates.put("ROLL", Float.valueOf(l[5]));
                missionList.add(ordinates);
            }
        } catch (IOException e) {
            String error = "Error reading mission file: " + e.getMessage();
            Toast.makeText(this, error, Toast.LENGTH_LONG).show();
            Log.e(TAG, error);
        }
        return missionList;
    }

    /**
     * Start the animation.
     *
     */
    private void startAnimation() {

        // stop the current animation timer
        stopAnimation();

        // calculate period from speed
        //int period = mSpeedSeekBar.getMax() - speed + 10;
        int period = 3;

        // create a timer to animate the tank
        mTimer = new Timer();
        mTimer.scheduleAtFixedRate(new TimerTask() {
            @Override public void run() {
                if (mMissionData == null) {
                    return;
                }
                // reset key frame at end of mission
                if (mKeyFrame >= mMissionData.size()) {
                    mKeyFrame = 0;
                }
                // animate the given key frame
                animate(mKeyFrame);
                mKeyFrame++;
            }
        }, 0, period);
    }

    /**
     * Stop the animation by canceling the timer.
     */
    private void stopAnimation() {
        if (mTimer != null) {
            mTimer.cancel();
        }
    }

    /**
     * Animates a single keyframe corresponding to the index in the mission data profile. Updates the position and
     * rotation of the 2D/3D plane graphic and sets the camera viewpoint.
     *动画对应于任务数据配置文件中的索引的单个关键帧。 更新2D / 3D平面图形的位置和旋转并设置摄像机视点。
     *
     * @param keyFrame index in mission data to show
     */
    private void animate(int keyFrame) {

        // get the next position from the mission data
        Map<String, Object> datum = mMissionData.get(keyFrame);
        Point position = (Point) datum.get("POSITION");

        // 右下角更新
        runOnUiThread(() -> {
            mCurrAltitude.setText(String.format("%.2f", position.getZ()));
            //mCurrHeading.setText(String.format("%.2f", (float) datum.get("HEADING")));
            //mCurrPitch.setText(String.format("%.2f", (float) datum.get("PITCH")));
            //mCurrRoll.setText(String.format("%.2f", (float) datum.get("ROLL")));
        });


        /** 更新3d模型飞机 */
        mPlane3D.setGeometry(position);
        mPlane3D.getAttributes().put("HEADING", datum.get("HEADING"));
        mPlane3D.getAttributes().put("PITCH", datum.get("PITCH"));
        mPlane3D.getAttributes().put("ROLL", datum.get("ROLL"));

    }

    /**
     * Switches between the orbiting camera controller and default globe camera controller.
     * 在轨道摄像机控制器和默认球形摄像机控制器之间切换
     */
    private void toggleFollow(boolean follow) {
        if (follow) {
            // set orbit camera controller 跟随模式
            mSceneView.setCameraController(mOrbitCameraController);
        } else {
            // set camera controller back to default 默认控制
            mSceneView.setCameraController(new GlobeCameraController());
        }
    }

    /** ok
     * Copy the given file from the app's assets folder to the app's cache directory.
     *将给定文件从应用程序的assets文件夹复制到应用程序的缓存目录。
     * @param fileName as String
     */
    private void copyFileFromAssetsToCache(String fileName) {
        AssetManager assetManager = getApplicationContext().getAssets();
        File file = new File(getCacheDir() + File.separator + fileName);
        if (!file.exists()) {
            try {
                InputStream in = assetManager.open(fileName);
                OutputStream out = new FileOutputStream(getCacheDir() + File.separator + fileName);
                byte[] buffer = new byte[1024];
                int read = in.read(buffer);
                while (read != -1) {
                    out.write(buffer, 0, read);
                    read = in.read(buffer);
                }
                Log.i(TAG, fileName + " copied to cache.");
            } catch (Exception e) {
                Log.e(TAG, "Error writing " + fileName + " to cache. " + e.getMessage());
            }
        } else {
            Log.i(TAG, fileName + " already in cache.");
        }
    }

    @Override
    protected void onPause() {
        mSceneView.pause();
        //mMapView.pause();
        mTimer.cancel();
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mSceneView.resume();
        // mMapView.resume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mSceneView.resume();
        // mMapView.dispose();
    }
}
