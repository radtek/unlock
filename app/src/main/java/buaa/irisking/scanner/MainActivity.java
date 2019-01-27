package buaa.irisking.scanner;

import java.io.File;
import java.io.FileInputStream;
import java.lang.ref.WeakReference;
import java.security.MessageDigest;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.fp.FingerprintManager.Fingerprint;
import com.fp.FingerprintManager.FingerprintManager;
import com.irisking.irisalgo.bean.IKEnrIdenStatus;
import com.irisking.irisalgo.util.AadharIrisISOFormat7;
import com.irisking.irisalgo.util.Config;
import com.irisking.irisalgo.util.EnrFeatrueStruct;
import com.irisking.irisalgo.util.EnumDeviceType;
import com.irisking.irisalgo.util.EnumEyeType;
import com.irisking.irisalgo.util.FeatureList;
import com.irisking.irisalgo.util.FileUtil;
import com.irisking.irisalgo.util.IKALGConstant;
import com.irisking.irisalgo.util.IrisInfo;
import com.irisking.irisalgo.util.Person;
import com.irisking.irisalgo.util.Preferences;
import buaa.irisking.irisapp.R;
import com.irisking.scanner.callback.CameraPreviewCallback;
import com.irisking.scanner.callback.IrisCaptureCallback;
import com.irisking.scanner.callback.IrisProcessCallback;
import com.irisking.scanner.model.EyePosition;
import com.irisking.scanner.presenter.IrisConfig;
import com.irisking.scanner.presenter.IrisPresenter;
import com.irisking.scanner.util.ImageUtil;
import com.irisking.scanner.util.TimeArray;

// 主文件，完成界面显示，UI控件控制等逻辑
@SuppressWarnings("unused")
public class MainActivity extends Activity implements OnClickListener {

	private TimeArray uvcTimeArray = new TimeArray();
	
	private String curName = "test";
	
	public boolean previewParaUpdated = false;
	
	// ============声音播放器=============
	//语音提示开关
    public SoundPool soundPool = null;
    private int frameIndex = 0;
    public int fartherId;
    public int closerId;
    public int enrosuccId;
    public int idensuccId;
    public int moveLeftId;
    public int moveRightId;
	//===================================

	//===============控件================
	private Button mIrisIdenBtn;
	private TextView mResultTextViewEnrRecFinal;
	private ImageView leftView; 
	private ImageView rightView; 
	private TextView mFrameRateTextView; // 帧率显示文本
	private IrisPresenter mIrisPresenter;
	private SurfaceView svCamera;
	private RoundProgressBar progressBar;
	private EyeView mEyeView; // 显示提示框的view界面
	private Button b1, b2, b3, b4,b5, b6 ,b7, b8, b9, b0, bBS; //数字键盘
	private static TextView pinCodeText; // PIN码显示
	//add by Haosu number Pad
	//add by yumingyuan for fingerprint
	private FingerprintManager theFpmanager = null;
	int fpVerifyWrongTimes = 0;//错误次数
	final int FP_VERIFY_WRONG_MAX = 5;//最大验证次数
	CancellationSignal cancelAuthenticateSignal = null;
	//add by yumingyuan for fingerprint
	//===================================
	
	//=========画IR图像=========
	private SurfaceHolder holder;
	private Matrix matrix;
	
	public  int eyeViewWidth = 0;
	public  int eyeViewHeight = 0;
	//==========================
	
	// ======load feature list==========
	FeatureList irisLeftData = new FeatureList();
	FeatureList irisRightData = new FeatureList();

	private int ledLevel = 4;
	
	public SqliteDataBase sqliteDataBase;
	
	private int irisMode = IKALGConstant.IR_IM_EYE_BOTH;
	private int maxFeatureCount = 900;
	//==================================
	 //屏幕中双眼的坐标位置
	private float eyeX1;
	private float eyeX2;
	private float eyeHeight;
	private float hor_scale;//横屏下缩放比例
	
	private IrisConfig.EnrollConfig mEnrollConfig;
	private IrisConfig.CaptureConfig mCaptureConfig;
	private IrisConfig.IdentifyConfig mIdentifyConfig;
	
	SharedPreferences sp;
	private String sp_name = "iris_sp_user";
	private String sp_count_name = "iris_sp_user_count";
    private boolean isStop;
    private static SurfaceHandler mSurfaceHandler;
    public static final int HANDLER_DRAW_IMAGE = 0x0010;
    public static final int HANDLER_UPDATE_TEXT = 0x0011;
    public static final int HANDLER_RESET_UI = 0x0012;
    public static final int HANDLER_RESET_PROGRESS = 0x0013;
    public static final int HANDLER_SHOW_LEFT = 0x0014;
    public static final int HANDLER_SHOW_RIGHT = 0x0015;
    
    private EnrFeatrueStruct leftECEyeFeat;
	private EnrFeatrueStruct rightECEyeFeat;

	//add by Hoasu
	private MyContextWrapper mContext;

	//解锁判断逻辑检查
	private boolean isIrisCheckOK;
	private boolean isPinCheckOK;
	private boolean isFPCheckOK;

	private int unlockMode; //解锁模式

	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mContext = new MyContextWrapper(this, "file");//改变虹膜数据存放地址/data/data/file/
        // addd by Hoasu
		sqliteDataBase = SqliteDataBase.getInstance(mContext);
		sp = this.getSharedPreferences(sp_name, Context.MODE_PRIVATE);

		isIrisCheckOK = false;
		isPinCheckOK = false;
		isFPCheckOK = false;
		unlockMode = 4; //默认口令解锁

		requestWindowFeature(Window.FEATURE_NO_TITLE); // 全屏，不出现图标
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
		getWindow().setFormat(PixelFormat.TRANSLUCENT);
		screenUiAdjust();
		
		setContentView(R.layout.activity_iris_recognition);
		
		initSound();
		initUI();
		//add by yumingyuan for fingerprint这样，一进入系统就允许指纹进行验证
		startFingerVerify();
		//add by yumingyuan for fingerprint
		mSurfaceHandler = new SurfaceHandler(MainActivity.this);
		if(Config.DEVICE_USBCAMERA){
			mIrisPresenter = new IrisPresenter(this, uvcPreviewCallback);
		} else if(Config.DEVICE_DOUBLECAMERA){
			
		} else{
			mIrisPresenter = new IrisPresenter(this, irPreviewCallback);
		}
		
		mEnrollConfig = new IrisConfig.EnrollConfig();
		mCaptureConfig = new IrisConfig.CaptureConfig();
		mIdentifyConfig = new IrisConfig.IdentifyConfig();
		initIrisData();
		iniConfigFile(); //读取配置文件
		doCheck(true);//开始检查
	}

	@Override
	protected void onStart() {
		mIrisPresenter.resume();
		isStop = false;
		super.onStart();
	}
	
	private void initSound() {
		soundPool = new SoundPool(1, AudioManager.STREAM_SYSTEM, 5);
        fartherId = soundPool.load(getApplicationContext(), R.raw.farther, 0);
        closerId = soundPool.load(getApplicationContext(), R.raw.closer, 0);
        enrosuccId = soundPool.load(getApplicationContext(), R.raw.enrsucc, 0);
        idensuccId = soundPool.load(getApplicationContext(), R.raw.idensucc, 0);
        moveLeftId = soundPool.load(getApplicationContext(), R.raw.moveleft, 0);
        moveRightId = soundPool.load(getApplicationContext(), R.raw.moveright, 0);
	}
	
	private void initIrisData() {
		// 2017.09.05 10:25修改，从数据库查询所有特征文件
		ArrayList<IrisUserInfo> leftEyeList = (ArrayList<IrisUserInfo>) sqliteDataBase.queryLeftFeature();
		ArrayList<IrisUserInfo> rightEyeList = (ArrayList<IrisUserInfo>) sqliteDataBase.queryRightFeature();

		if ((leftEyeList == null || leftEyeList.size() == 0) && (rightEyeList == null || rightEyeList.size() == 0)) {
			return;
		}
		irisLeftData.clear();
		irisRightData.clear();
		for (int i = 0; i < leftEyeList.size(); i++) {
			irisLeftData.add(new Person(leftEyeList.get(i).m_UserName,leftEyeList.get(i).m_Uid, 1), EnumEyeType.LEFT,
					leftEyeList.get(i).m_LeftTemplate);
		}

		for (int i = 0; i < rightEyeList.size(); i++) {
			irisRightData.add(new Person(rightEyeList.get(i).m_UserName,rightEyeList.get(i).m_Uid, 1), EnumEyeType.RIGHT,
					rightEyeList.get(i).m_RightTemplate);
		}

		mIrisPresenter.setIrisData(irisLeftData, irisRightData, null);//需要把特征传入jar包，以便识别
	}
	
	@Override
	protected void onStop() {
		isStop = true;
		if(mSurfaceHandler != null){
			mSurfaceHandler.removeCallbacksAndMessages(null);
		}
		resetUI();
		mIrisPresenter.pause();
		super.onStop();
	}
	
	@Override
	public void onLowMemory() {
		super.onLowMemory();
	}

	private int[] textureBuffer;
	private Bitmap gBitmap;
	private byte[] bmpData;
	private int bmpWidth;
	private int bmpHeight;
	
	private void drawImage() {
		if(bmpWidth == 0 || bmpHeight == 0) return;
		if(textureBuffer == null){
			textureBuffer = new int[bmpWidth * bmpHeight];
		}
		if(gBitmap == null){
			gBitmap = Bitmap.createBitmap(bmpWidth, bmpHeight, Bitmap.Config.RGB_565);//8 位 RGB位图,没有透明度
		}

		ImageUtil.getBitmap8888(bmpData, bmpHeight, bmpWidth, 0, 0, bmpWidth-1, bmpHeight-1, textureBuffer, 0, 1);
		
		gBitmap.setPixels(textureBuffer, 0, bmpWidth, 0, 0, bmpWidth, bmpHeight);
		
		Canvas canvas = holder.lockCanvas();
		if(canvas != null){
			if(EnumDeviceType.isSpecificDevice(EnumDeviceType.LONGKE) || EnumDeviceType.isSpecificDevice(EnumDeviceType.YLT_BM5300)
					|| EnumDeviceType.isSpecificDevice(EnumDeviceType.HCTX_LS_5512)){
				canvas.scale(1, 1, eyeViewWidth / 2.0f, eyeViewHeight / 2.0f);
			}else{
				canvas.scale(-1, 1, eyeViewWidth / 2.0f, eyeViewHeight / 2.0f);
			}
			canvas.drawBitmap(gBitmap, matrix, null);
			holder.unlockCanvasAndPost(canvas);
		}
	}
	/**
	 * 屏幕UI调整
	 */
	private void screenUiAdjust() {
		DisplayMetrics metrics = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(metrics);
		
		int screenWidth = metrics.widthPixels; // 获取屏幕的宽
		int transWid = 0;

		Configuration mConfiguration = this.getResources().getConfiguration();
		
		int ori = mConfiguration.orientation;// 获取屏幕方向
		if (ori == Configuration.ORIENTATION_LANDSCAPE) {	// 如果是横屏，预览区域的宽为当前屏幕宽的80%，根据设备的不同可以动态再进行适配
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
			hor_scale = 0.6f;
			eyeViewWidth = (int) (screenWidth * hor_scale);
			transWid = (screenWidth - eyeViewWidth) / 2;
			
		} else if (ori == Configuration.ORIENTATION_PORTRAIT) {	// 竖屏
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
			hor_scale = 1.0f;
			eyeViewWidth = screenWidth;// 如果是竖屏，预览区域的宽为屏幕的宽
			transWid = 0;
		}
		
		// 由于图像是16:9的图像
		eyeViewHeight = (int) (eyeViewWidth / 1.777f);
		// 在480*270的分辨率下，双眼相对于左上角的坐标点为（140,110），（340,110） ps：固定坐标点，修改需要咨询虹霸开发人员
		float x = (float) eyeViewWidth / IKALGConstant.IK_DISPLAY_IMG_WIDTH;
		float y = (float) eyeViewHeight / IKALGConstant.IK_DISPLAY_IMG_HEIGHT;
		DecimalFormat df = new DecimalFormat("0.00");
		eyeX1 = Float.parseFloat(df.format(x)) * EnumDeviceType.getCurrentDevice().getDefaultLeftIrisCol()+ transWid;
		eyeX2 = Float.parseFloat(df.format(x)) * EnumDeviceType.getCurrentDevice().getDefaultRightIrisCol()+ transWid + 50;
		eyeHeight = Float.parseFloat(df.format(y)) * EnumDeviceType.getCurrentDevice().getDefaultLeftIrisRow();
	}

	private static class SurfaceHandler extends Handler {
        private WeakReference<MainActivity> handlerReference;

        public SurfaceHandler(MainActivity activity) {
            handlerReference = new WeakReference<MainActivity>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
        	MainActivity activity = handlerReference == null ? null : handlerReference.get();
            if (activity.isStop) {
                return;
            }
            if (activity == null || activity.isFinishing()) {
                return;
            }
            if (msg.what == HANDLER_DRAW_IMAGE) {
                activity.drawImage();
            }
            if (msg.what == HANDLER_UPDATE_TEXT) {
            	activity.mResultTextViewEnrRecFinal.setText(msg.obj.toString());
            }
        	if (msg.what == HANDLER_RESET_UI) {
        		activity.resetUI();
        	}
        	if( msg.what == HANDLER_RESET_PROGRESS){
        		activity.screenUiAdjust();
        		activity.progressBar.setXAndY(activity.eyeX1, activity.eyeX2, activity.eyeHeight);// 设置双眼progressbar的位置
        		activity.progressBar.invalidate();
        	}
        	if(msg.what == HANDLER_SHOW_LEFT){
        		Bitmap leftBm = (Bitmap) msg.obj;
        		activity.leftView.setImageBitmap(leftBm);
        	}
        	if(msg.what == HANDLER_SHOW_RIGHT){
        		Bitmap rightBm = (Bitmap) msg.obj;
        		activity.rightView.setImageBitmap(rightBm);
        	}
        	
        }
    }

    private SurfaceHolder.Callback surfaceCallback = new SurfaceHolder.Callback() {
        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            Canvas canvas = holder.lockCanvas();
            canvas.drawColor(Color.rgb(0, 0, 0));
            holder.unlockCanvasAndPost(canvas);
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
        }
    };
	protected int distRange = 0;

	// Init view
	private void initUI() {
		svCamera = (SurfaceView) findViewById(R.id.iv_camera);
		LayoutParams svParams = svCamera.getLayoutParams();
		svParams.width = eyeViewWidth;
		svParams.height = eyeViewHeight;
		svCamera.setLayoutParams(svParams);
		
		mEyeView = (EyeView) findViewById(R.id.eye);

		holder = svCamera.getHolder();
		holder.addCallback(surfaceCallback);
		matrix = new Matrix();

		progressBar = (RoundProgressBar) findViewById(R.id.roundProgress);
		progressBar.setXAndY(eyeX1, eyeX2, eyeHeight);// 设置双眼progressbar的位置
		progressBar.setHorScale(hor_scale);
	// Init button
		b1 = (Button) findViewById(R.id.button1);
		b1.setOnClickListener(this);

		b2 = (Button) findViewById(R.id.button2);
		b2.setOnClickListener(this);

		b3 = (Button) findViewById(R.id.button3);
		b3.setOnClickListener(this);

		b4 = (Button) findViewById(R.id.button4);
		b4.setOnClickListener(this);

		b5 = (Button) findViewById(R.id.button5);
		b5.setOnClickListener(this);

		b6 = (Button) findViewById(R.id.button6);
		b6.setOnClickListener(this);

		b7 = (Button) findViewById(R.id.button7);
		b7.setOnClickListener(this);

		b8 = (Button) findViewById(R.id.button8);
		b8.setOnClickListener(this);

		b9 = (Button) findViewById(R.id.button9);
		b9.setOnClickListener(this);

		b0 = (Button) findViewById(R.id.button0);
		b0.setOnClickListener(this);

		bBS = (Button) findViewById(R.id.buttonBS);
		bBS.setOnClickListener(this);

		mIrisIdenBtn = (Button) findViewById(R.id.bt_unlock);
		mIrisIdenBtn.setOnClickListener(this);

		pinCodeText = (TextView) findViewById(R.id.pincodeText);


		mResultTextViewEnrRecFinal = (TextView) findViewById(R.id.ie_final_result);

		leftView = (ImageView) findViewById(R.id.iv_left);
		rightView = (ImageView) findViewById(R.id.iv_right);

		previewParaUpdated = false;

		if(Config.DEVICE_SINGLE_EYE){
			progressBar.setVisibility(View.GONE);
			mEyeView.setVisibility(View.VISIBLE);
			irisMode = IKALGConstant.IR_IM_EYE_LEFT;
		}
	}
	
	private boolean isActive = false;
	Runnable cliRunnable = new Runnable() {
		
		@Override
		public void run() {
			//mIrisRegisterBtn.performClick();
		}
	};
	@Override
	public void onClick(View v) {
		EnumDeviceType.getCurrentDevice().setParam(ledLevel);
		switch (v.getId()) {
			case R.id.bt_unlock:
				doCheck(false);
				break;
			case R.id.buttonBS:
				pinCodeText.setText("");
				break;
			case R.id.button0:
				pinCodeText.append("0");
				break;
			case R.id.button1:
				pinCodeText.append("1");
				break;
			case R.id.button2:
				pinCodeText.append("2");
				break;
			case R.id.button3:
				pinCodeText.append("3");
				break;
			case R.id.button4:
				pinCodeText.append("4");
				break;
			case R.id.button5:
				pinCodeText.append("5");
				break;
			case R.id.button6:
				pinCodeText.append("6");
				break;
			case R.id.button7:
				pinCodeText.append("7");
				break;
			case R.id.button8:
				pinCodeText.append("8");
				break;
			case R.id.button9:
				pinCodeText.append("9");
				break;
		}
	}

	/**
	 * 读取多因子解锁配置文件
	 */
	public void iniConfigFile() {
		String conFile = "/data/data/file/Authconfig.properties";
		try{
			File f=new File(conFile);
			if(f.exists()){
				//读取配置文件
				FileInputStream fin = new FileInputStream(conFile);
				Properties pro = new Properties();
				pro.load(fin);
				String auth_level = pro.getProperty("authentication_level");
				unlockMode = Integer.parseInt(auth_level.trim());
			}
		}
		catch (Exception e){
			e.printStackTrace();
		}

	}


	/**
	检查解锁逻辑并解锁
	 程序点击解锁按钮就会执行此方法
 	*/
	public void doCheck(boolean iniStart){

		// TODO: 2019/1/25 需要添加解锁逻辑

		switch(unlockMode) {
			case 4: //pin码解锁
			{
				if (!isPinCheckOK){
					if (iniStart)
						Toast.makeText(MainActivity.this, "请输入口令解锁", Toast.LENGTH_SHORT).show();
					else
						doPinCheck();
				}else {
						resetUI();
						System.exit(0);//解锁OK后退出
					}
			}
			break;

			case 2://指纹或pin码解锁
				{
					if(iniStart)
						Toast.makeText(MainActivity.this,"请使用指纹或口令解锁", Toast.LENGTH_SHORT).show();
					if(!isFPCheckOK) {
						Toast.makeText(MainActivity.this, "请验证指纹", Toast.LENGTH_SHORT).show();
						doFPCheck();
					}
					if(!isPinCheckOK) {
						Toast.makeText(MainActivity.this, "请输入口令", Toast.LENGTH_SHORT).show();
						if(!iniStart)
							doPinCheck();
					}
					if(isFPCheckOK || isPinCheckOK ){
						resetUI();
						System.exit(0);//解锁OK后退出
					}

			}
			break;

			case 1: //虹膜或pin码解锁
				{
					if(iniStart)
						Toast.makeText(MainActivity.this,"请使用虹膜或口令解锁", Toast.LENGTH_SHORT).show();
					if(!isIrisCheckOK) {
						Toast.makeText(MainActivity.this, "请验证虹膜", Toast.LENGTH_SHORT).show();
						irisCheck();
					}
					if(!isPinCheckOK) {
						Toast.makeText(MainActivity.this, "请输入口令", Toast.LENGTH_SHORT).show();
						if(!iniStart)
							doPinCheck();
					}
					if(isIrisCheckOK || isPinCheckOK){
						resetUI();
						System.exit(0);//解锁OK后退出
					}

			}
			break;

			case 6: //指纹+口令解锁
				{
					if(iniStart)
						Toast.makeText(MainActivity.this,"请使用指纹+口令解锁", Toast.LENGTH_SHORT).show();
					if(!isFPCheckOK) {
						Toast.makeText(MainActivity.this, "请验证指纹", Toast.LENGTH_SHORT).show();
						doFPCheck();
					} else if(!isPinCheckOK) {
						Toast.makeText(MainActivity.this, "请输入口令", Toast.LENGTH_SHORT).show();
						if(!iniStart)
							doPinCheck();
					}
					if(isFPCheckOK && isPinCheckOK ){ //指纹+口令解锁
						resetUI();
						System.exit(0);//解锁OK后退出
					}
			}
			break;

			case 5: //虹膜+口令解锁
				{
					if(iniStart)
						Toast.makeText(MainActivity.this,"请使用虹膜+口令解锁", Toast.LENGTH_SHORT).show();
					if(!isIrisCheckOK) {
						Toast.makeText(MainActivity.this, "请验证虹膜", Toast.LENGTH_SHORT).show();
						irisCheck();
					} else if(!isPinCheckOK) {
						Toast.makeText(MainActivity.this, "请输入口令", Toast.LENGTH_SHORT).show();
						if(!iniStart)
							doPinCheck();
					}
					if(isIrisCheckOK && isPinCheckOK){
						resetUI();
						System.exit(0);//解锁OK后退出
					}
			}
			break;

			case 7://虹膜+指纹+口令解锁
				{
					if(iniStart)
						Toast.makeText(MainActivity.this,"请使用虹膜+指纹+口令解锁", Toast.LENGTH_SHORT).show();
					if(!isIrisCheckOK) {
						Toast.makeText(MainActivity.this, "请验证虹膜", Toast.LENGTH_SHORT).show();
						irisCheck();
					}else if(!isFPCheckOK) {
						Toast.makeText(MainActivity.this, "请验证指纹", Toast.LENGTH_SHORT).show();
						doFPCheck();
					}else if(!isPinCheckOK) {
						Toast.makeText(MainActivity.this, "请输入口令", Toast.LENGTH_SHORT).show();
						if(!iniStart)
							doPinCheck();
					}

					if(isIrisCheckOK && isPinCheckOK && isFPCheckOK){
						resetUI();
						System.exit(0);//解锁OK后退出
					}
			}
			break;

			default: //pin码解锁
			{
				if (!isPinCheckOK){
					if (iniStart)
						Toast.makeText(MainActivity.this, "请输入口令解锁", Toast.LENGTH_SHORT).show();
					else
						doPinCheck();
				}else {
					resetUI();
					System.exit(0);//解锁OK后退出
				}
			}
				break;
		}

	}

	/**
	检查PIN码
	 */
	public void doPinCheck(){
		String mPin = pinCodeText.getText().toString();
		File dataDirectory = Environment.getDataDirectory();
		String conFile = dataDirectory.toString() + "/data/userconfig.properties";
		try{
			File f=new File(conFile);
			if(f.exists()){
				//读取配置文件
				FileInputStream fin = new FileInputStream(conFile);
				Properties pro = new Properties();
				pro.load(fin);
				String uPin = pro.getProperty("userpass");
				uPin = uPin.trim();
				MessageDigest digest = MessageDigest.getInstance("SHA-1");
				byte[] result = digest.digest(mPin.getBytes());
				if(convertHashToString(result).equals(uPin)) {
					isPinCheckOK = true;
				}
				else {
					Toast.makeText(this,"PIN码错误",Toast.LENGTH_LONG).show();
				}
			}else
				Toast.makeText(this,"请联系管理员配置主机！",Toast.LENGTH_LONG).show();
			pinCodeText.setText("");
		}
		catch (Exception e){
			pinCodeText.setText("");
			e.printStackTrace();
		}
	}

	//add by yumingyuan 20190117将byte[]转换为16进制字符串
	private static String convertHashToString(byte[] hashBytes) {
		String returnVal = "";
		for (int i = 0; i < hashBytes.length; i++) {
			returnVal += Integer.toString(( hashBytes[i] & 0xff) + 0x100, 16).substring(1);
		}
		return returnVal.toLowerCase();
	}


	/**
	 检查指纹
	 */
	public void doFPCheck(){
		startFingerVerify();//调用指纹认证函数
	}


	/**
	开始虹膜检测
	 */
	public void irisCheck(){
		if(isActive){
			resetUI();
		}else{
			isActive = true;
			svCamera.setKeepScreenOn(true);
			mIdentifyConfig.irisMode = IKALGConstant.IR_IM_EYE_UNDEF;
			mIdentifyConfig.overTime = 30;
			mIrisPresenter.startIdentify(mIdentifyConfig, processCallback);
		}
	}

	
	public void resetUI(){
		isActive = false;
		maxLeft = 0;
		maxRight = 0;
		mResultTextViewEnrRecFinal.setText(" ");
		svCamera.setKeepScreenOn(false);
		mIrisIdenBtn.setText(R.string.start_identify);
		mIrisIdenBtn.setEnabled(true);
		mIrisPresenter.stopAlgo();
		progressBar.setLeftAndRightProgress(0, 0, 0);
		mEyeView.eyeDetectView.reset();
		mEyeView.eyeDetectView.setProgress(0, 0, 0, 0, false,false);
		mEyeView.postInvalidate();
		pinCodeText.setText("");
	}
	
	private void updateUIStatus(int status){
		
		String tips = "";
		int m_curDist = IKEnrIdenStatus.getInstance().irisPos.dist;
		
		switch (status) {
		case IKALGConstant.IRIS_FRAME_STATUS_BLINK:
			tips = getString(R.string.tip_blink_eyes);
			break;
		case IKALGConstant.IRIS_FRAME_STATUS_MOTION_BLUR:
		case IKALGConstant.IRIS_FRAME_STATUS_FOCUS_BLUR:
			tips = getString(R.string.tip_keep_stable);
			break;
		case IKALGConstant.IRIS_FRAME_STATUS_BAD_EYE_OPENNESS:
			tips = getString(R.string.tip_open_eye);
			break;
		case IKALGConstant.IRIS_FRAME_STATUS_WITH_GLASS:
			tips = getString(R.string.tip_remove_glasses);
			break;
		case IKALGConstant.IRIS_FRAME_STATUS_WITH_GLASS_HEADUP:
			tips = getString(R.string.tip_raise_head);
			break;
		case IKALGConstant.IRIS_FRAME_STATUS_WITH_GLASS_HEADDOWN:
			tips = getString(R.string.tip_lower_head);
			break;
		case IKALGConstant.IRIS_FRAME_STATUS_EYE_TOO_CLOSE:
			int suit = EnumDeviceType.getCurrentDevice().getSuitablePosDist();
			int movedist = Math.abs(m_curDist - suit);	
			if (m_curDist != -1) {
				tips = String.format(getString(R.string.tip_move_father_dist), movedist);
			} else {
				tips = getString(R.string.tip_move_father);
			}
			soundPlay(fartherId);
			break;
		case IKALGConstant.IRIS_FRAME_STATUS_EYE_TOO_FAR:
			suit = EnumDeviceType.getCurrentDevice().getSuitablePosDist();
			movedist = Math.abs(m_curDist - suit);
			if (m_curDist != -1) {
				tips = String.format(getString(R.string.tip_move_closer_dist), movedist); 
			} else{
				tips = getString(R.string.tip_move_closer);
			}
			soundPlay(closerId);
			break;
		case IKALGConstant.IRIS_FRAME_STATUS_EYE_NOT_FOUND:
			tips = getString(R.string.tip_noeyedetected);
			break;
		case IKALGConstant.IRIS_FRAME_STATUS_UNAUTHORIZED_ATTACK:
			tips = getString(R.string.tip_unauthorized_attack);
			break;
		case IKALGConstant.IRIS_FRAME_STATUS_CONTACTLENS:
			tips = getString(R.string.tip_remove_contact_lenses);
			break;
		case IKALGConstant.IRIS_FRAME_STATUS_ATTACK:
			tips = getString(R.string.tip_do_not_attack);
			break;
		case IKALGConstant.IRIS_FRAME_STATUS_OUTDOOR:
			tips = getString(R.string.tip_use_indoors);
			break;
		case IKALGConstant.IRIS_FRAME_STATUS_EYE_TOO_UP:
			tips = getString(R.string.tip_bad_image);
			break;
		case IKALGConstant.IRIS_FRAME_STATUS_EYE_TOO_DOWN:
			tips = getString(R.string.tip_bad_image);
			break;
		case IKALGConstant.IRIS_FRAME_STATUS_EYE_TOO_LEFT:
			tips = getString(R.string.tip_bad_image);
			soundPlay(moveRightId);
			break;
		case IKALGConstant.IRIS_FRAME_STATUS_EYE_TOO_RIGHT:
			tips = getString(R.string.tip_bad_image);
			soundPlay(moveLeftId);
			break;
		case IKALGConstant.IRIS_FRAME_STATUS_SUITABLE:
			tips = getString(R.string.tip_scanning);
			break;
		case IKALGConstant.IRIS_FRAME_STATUS_BAD_IMAGE_QUALITY:
			tips = getString(R.string.tip_bad_image);
			break;
		case IKALGConstant.ERR_INVALIDDATE:
			tips = getString(R.string.tip_invaliddate);
			break;
		case IKALGConstant.ERR_INVALIDDEVICE:
			tips = getString(R.string.tip_invaliddevice);
			break;
		default:
			break;
		}
		Message msg = Message.obtain();
		msg.obj = tips;
		msg.what = HANDLER_UPDATE_TEXT;
		mSurfaceHandler.sendMessage(msg);
	}
	
	private IrisCaptureCallback captureCallback = new IrisCaptureCallback() {
		
		@Override
		public void onUIStatusUpdate(int status){
			updateUIStatus(status);
		}
		
		@Override
		public void onCaptureProgress(int currentLeftCount, int currentRightCount, int needCount) {
		}

		@Override
		public void onCaptureComplete(int ifSuccess,EnrFeatrueStruct leftEyeFeat, EnrFeatrueStruct rightEyeFeat,EnrFeatrueStruct faceFeat) {
		}

		@Override
		public void onEyeDetected(boolean isValid,EyePosition leftPos,EyePosition rightPos,int captureDistance) {
			mEyeView.eyeDetectView.init(
				EnumDeviceType.getCurrentDevice().getRotateAngle(), 
				EnumDeviceType.getCurrentDevice().getPreviewWidth(), 
				EnumDeviceType.getCurrentDevice().getPreviewHeight(), 
				eyeViewWidth, eyeViewHeight);

			mEyeView.eyeDetectView.setDetectResult(leftPos, rightPos, distRange);
			mEyeView.invalidate();	// 画人员定位圆
		}

		@Override
		public void onAlgoExit() {
			if(mSurfaceHandler != null){
				mSurfaceHandler.sendEmptyMessage(HANDLER_RESET_UI);
			}
		}
	};
	

	
	private int maxLeft = 0;
	private int maxRight = 0;
	private IrisProcessCallback processCallback = new IrisProcessCallback() {
		@Override
		public void onUIStatusUpdate(int status){
			updateUIStatus(status);
		}
		
		@Override
		public void onEnrollProgress(int currentLeftCount, int currentRightCount, int needCount) {

		}

		@Override
		public void onEnrollComplete(int ifSuccess,EnrFeatrueStruct leftEyeFeat, EnrFeatrueStruct rightEyeFeat,EnrFeatrueStruct faceFeat) {
		}

		@Override
		public void onEyeDetected(boolean isValid, EyePosition leftPos, EyePosition rightPos, int captureDistance) {
		}

		@Override
		public void onIdentifyComplete(int ifSuccess, int matchIndex, int eyeFlag) {
			if(mSurfaceHandler!=null){
				mSurfaceHandler.sendEmptyMessage(HANDLER_RESET_UI);
			}
			if (ifSuccess != IKALGConstant.ALGSUCCESS) {
				AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);

				if (ifSuccess == IKALGConstant.ERR_IDENFAILED) {
					builder.setMessage(R.string.dialog_identification_failed);
				} else if (ifSuccess == IKALGConstant.ERR_OVERTIME) {
					builder.setMessage(R.string.dialog_timeout);
				} else if (ifSuccess == IKALGConstant.ERR_NOFEATURE) {
					builder.setMessage(R.string.dialog_no_feature);
				} else if (ifSuccess == IKALGConstant.ERR_EXCEEDMAXMATCHCAPACITY) {
					builder.setMessage(R.string.dialog_overmuch_feature);
				} else if (ifSuccess == IKALGConstant.ERR_IDEN) {
					builder.setMessage(R.string.dialog_identification_failed);
				} else{
					builder.setMessage("error code:" + ifSuccess);
				}

				builder.setTitle(R.string.dialog_title_notice);
				builder.setCancelable(false);
				builder.setPositiveButton(R.string.dialog_button_ok,
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								dialog.dismiss();
							}
						});
				builder.create().show();

				return;
			}
			soundPool.play(idensuccId, 1, 1, 1, 0, 1);
			
			String matchName = "";
			if(eyeFlag == EnumEyeType.LEFT){
				matchName = irisLeftData.personAt(matchIndex).getName();
			}else{
				matchName = irisRightData.personAt(matchIndex).getName();
			}
			Toast.makeText(getApplicationContext(),"虹膜识别成功", Toast.LENGTH_SHORT).show();
			isIrisCheckOK = true;
		}

		@Override
		public void onAlgoExit() {
			if(mSurfaceHandler!=null){
				mSurfaceHandler.sendEmptyMessage(HANDLER_RESET_UI);
			}
		}
	};

	
	Runnable handleISODataRunnable = new Runnable() {
		
		@Override
		public void run() {
			IrisInfo leftFirstIris = leftECEyeFeat.irisInfo[0];
			IrisInfo rightFirstIris = rightECEyeFeat.irisInfo[0];
			
			String stqcPath = Environment.getExternalStorageDirectory() + "/PidData/";
			
			AadharIrisISOFormat7 irisIsoFormat7 = new AadharIrisISOFormat7();
			if(leftFirstIris != null){
				byte[] isoIrisData = ImageUtil.compress(leftFirstIris.isoIrisData, leftFirstIris.isoWidth, leftFirstIris.isoHeight);
				byte[] isoImgLeft = irisIsoFormat7.getISOFormatHeader(
						isoIrisData, (short) 0,
						(short) leftFirstIris.isoHeight,
						(short) leftFirstIris.isoWidth,
						isoIrisData.length);
//				FileUtil.saveData(isoIrisData, stqcPath , "leftISO.j2k");	
//				FileUtil.saveData(isoImgLeft, stqcPath, "iso_left.iso");
				
				byte[] deCompress = ImageUtil.deCompress(isoIrisData,ImageUtil.compressRate);
				byte[] convertToBitmapArray = FileUtil.convertToBitmapArray(deCompress, leftFirstIris.isoHeight, leftFirstIris.isoWidth);
				Bitmap leftBm = BitmapFactory.decodeByteArray(convertToBitmapArray, 0, convertToBitmapArray.length);
				if(!isStop && mSurfaceHandler != null){
					Message msg = Message.obtain();
					msg.obj = leftBm;
					msg.what = HANDLER_SHOW_LEFT;
					mSurfaceHandler.sendMessage(msg);
				}
			}
			if(rightFirstIris != null){
				byte[] isoIrisData = ImageUtil.compress(rightFirstIris.isoIrisData, rightFirstIris.isoWidth, rightFirstIris.isoHeight);
				byte[] isoImgRight = irisIsoFormat7.getISOFormatHeader(
						isoIrisData, (short) 0,
						(short) rightFirstIris.isoHeight,
						(short) rightFirstIris.isoWidth,
						isoIrisData.length);
//				FileUtil.saveData(isoIrisData, stqcPath , "rightISO.j2k");	
//				FileUtil.saveData(isoImgRight, stqcPath, "iso_right.iso");
				
				byte[] deCompress = ImageUtil.deCompress(isoIrisData,ImageUtil.compressRate);
				byte[] convertToBitmapArray = FileUtil.convertToBitmapArray(deCompress, rightFirstIris.isoHeight, rightFirstIris.isoWidth);
				Bitmap rightBm = BitmapFactory.decodeByteArray(convertToBitmapArray, 0, convertToBitmapArray.length);
				if(!isStop && mSurfaceHandler != null){
					Message msg = Message.obtain();
					msg.obj = rightBm;
					msg.what = HANDLER_SHOW_RIGHT;
					mSurfaceHandler.sendMessage(msg);
				}
			}
		}
	};
	
	private CameraPreviewCallback irPreviewCallback = new CameraPreviewCallback.IRPreviewCallback(){
		@Override
		public void onPreviewFrame(byte[] bmpData, int bmpWidth, int bmpHeight) {
			
			uvcTimeArray.newTime();
			if (uvcTimeArray.count() % 3 == 0) {
				Log.e("iris_verbose","MainActivity onPreviewFrame fps:" + uvcTimeArray.toString() + ", isStop:" + isStop);
			}
			
			MainActivity.this.bmpWidth = bmpWidth;
			MainActivity.this.bmpHeight = bmpHeight;
			MainActivity.this.bmpData = bmpData;
			
			if(previewParaUpdated == false){
				previewParaUpdated = true;
    			
				if(matrix != null){
					matrix.postScale(1.0f*eyeViewWidth/bmpWidth, 1.0f*eyeViewHeight/bmpHeight);
				}
			}
			
			if (!isStop && mSurfaceHandler != null ) {
                mSurfaceHandler.sendEmptyMessage(HANDLER_DRAW_IMAGE);
            }
		}
	};
	
	private CameraPreviewCallback uvcPreviewCallback = new CameraPreviewCallback.UVCPreviewCallback(){
		@Override
		public void onPreviewFrame(byte[] bmpData, int bmpWidth, int bmpHeight) {
			
			uvcTimeArray.newTime();
			if (uvcTimeArray.count() % 3 == 0) {
				Log.e("iris_verbose","MainActivity onPreviewFrame fps:" + uvcTimeArray.toString());
			}
			
			MainActivity.this.bmpWidth = bmpWidth;
			MainActivity.this.bmpHeight = bmpHeight;
			MainActivity.this.bmpData = bmpData;
			
			if(previewParaUpdated == false){
				previewParaUpdated = true;
				if(matrix != null){
					matrix.postScale(1.0f*eyeViewWidth/bmpWidth, 1.0f*eyeViewHeight/bmpHeight);
				}
			}
			
			if (!isStop && mSurfaceHandler != null ) {
                mSurfaceHandler.sendEmptyMessage(HANDLER_DRAW_IMAGE);
            }
		}

		@Override
		public void onCameraConnected() {
			if(mSurfaceHandler!=null){
				mSurfaceHandler.sendEmptyMessage(HANDLER_RESET_PROGRESS);
			}
		}

		@Override
		public void onCameraDisconnected() {
			if(mSurfaceHandler!=null){
				mSurfaceHandler.sendEmptyMessage(HANDLER_RESET_UI);
			}
			Toast.makeText(MainActivity.this, R.string.dialog_usb_disconnected, Toast.LENGTH_SHORT).show();
		}
		@Override
		public void onCameraDettached() {
			if(mSurfaceHandler!=null){
				mSurfaceHandler.sendEmptyMessage(HANDLER_RESET_UI);
			}
			Toast.makeText(MainActivity.this, R.string.dialog_usb_disconnected, Toast.LENGTH_SHORT).show();
		}

		@Override
		public void onDeviceFlip(boolean isFlip) {
			if(isFlip && mSurfaceHandler!=null){
				mSurfaceHandler.sendEmptyMessage(HANDLER_RESET_UI);
			}
		}
	};

	public void onBackPressed() {
		resetUI();
		super.onBackPressed();
	}
	
	@Override
	protected void onDestroy() {
		if(mSurfaceHandler != null){
			mSurfaceHandler.removeCallbacksAndMessages(null);
			mSurfaceHandler = null;
		}
		ThreadPoolProxyFactory.getNormalThreadPoolProxy().remove(handleISODataRunnable);
		
		if(soundPool != null){
            soundPool.autoPause();
            soundPool.release();
            soundPool = null;
        }
		if(mIrisPresenter!=null){
			mIrisPresenter.release();
			mIrisPresenter = null;
		}
		super.onDestroy();
	}

	private void soundPlay(int soundId) {
        frameIndex++;
        if ((frameIndex % 60 == 0) && (soundPool != null)) {
            soundPool.play(soundId, 1, 1, 1, 0, 1);
            frameIndex = 0;
        }
    }

	//add by yumingyuan 20190118重写onkeydown方法，捕捉keycode，返回false则屏蔽按键
	public boolean onKeyDown( int keyCode, KeyEvent event) {
		//System.out.println(keyCode);
		if (keyCode == 4) {
			return false;
		}
		if(keyCode==5)
		{
			System.out.println(event.getKeyCode());
			return false;
		}
		return super.onKeyDown(keyCode, event);
	}
	//add by yumingyuan for fingerprint 20190118
	private FingerprintManager getFpManager(){
		if(theFpmanager == null){
			theFpmanager = FingerprintManager.getFpManager(this);//获得指纹管理器服务对象
		}
		return theFpmanager;
	}
	//add by yumingyuan for fingerprint
	private void startFingerVerify() {//启动指纹认证服务
		getFpManager();
		// 获取指纹数
		List<Fingerprint> fpList = theFpmanager.getEnrolledFingerprints();
		int iEnrollFingerCnt = (fpList != null) ? fpList.size() : 0;//双目运算符，获得指纹数信息

		// 当前指纹数>0 则配置并开启指纹认证
		if(iEnrollFingerCnt > 0){
			cancelAuthenticateSignal = new CancellationSignal();
			theFpmanager.authenticate(null, cancelAuthenticateSignal, 0, mAuthCB, null);//启动指纹识别，指定回调函数mAuthCB
		}
	}
	private void fpAuthenticationFailed(){
		fpVerifyWrongTimes++;//错误次数
		if(fpVerifyWrongTimes < FP_VERIFY_WRONG_MAX){
			mHandlerFp.sendMessage(mHandlerFp.obtainMessage(2,0, 0));//发送重试消息
		}else{
			//已经完全失败了
			mHandlerFp.sendMessage(mHandlerFp.obtainMessage(3,0, 0));//发送失败消息
		}
	}
	FingerprintManager.AuthenticationCallback mAuthCB = new  FingerprintManager.AuthenticationCallback(){

		//认证出错
		public void onAuthenticationError(int errorCode, CharSequence errString) {
			if(errorCode != FingerprintManager.FINGERPRINT_ERROR_CANCELED){
				fpAuthenticationFailed();//调用认证失败函数
			}
		}

		public void onAuthenticationHelp(int helpCode, CharSequence helpString) {
			fpAuthenticationFailed();//调用认证失败处理函数
		}
		//认证成功, 通过result参数 识别当前成功认证的是那个指纹
		public void onAuthenticationSucceeded(FingerprintManager.AuthenticationResult result) {
			cancelAuthenticateSignal = null;
			System.out.println("OKOK!");
			mHandlerFp.sendMessage(mHandlerFp.obtainMessage(1, 0, 0));//向主线程（真正的程序）发信号
		}
		//认证失败
		public void onAuthenticationFailed() {
			fpAuthenticationFailed();
		}

		public void onAuthenticationAcquired(int acquireInfo) {

		}
	};
	//主线程处理函数，mHandlerFp接收指纹验证线程发送的信号
	Handler mHandlerFp = new Handler() {
		public void handleMessage(Message msg) {
			switch(msg.what) {
				case 1: //fp success
				{
					Toast.makeText(MainActivity.this,"指纹验证成功", Toast.LENGTH_SHORT).show();
					fpVerifyWrongTimes = 0;
					System.out.println("verify OK!");
					isFPCheckOK=true;
					/*if(Authentication_level==2)//单独指纹的情况直接解锁
					{
						System.exit(0);
					}else if(Authentication_level==6)//指纹和PIN码的情况，暂不解锁，等待pin码输入
					{
						fp_status=true;
						Toast.makeText(MainActivity.this,"指纹验证成功，请输入PIN码", Toast.LENGTH_SHORT).show();
					}else if(Authentication_level==7)//指纹虹膜，PIN的情况
					{
						fp_status=true;
						Toast.makeText(MainActivity.this,"指纹验证成功，请输入PIN码并进行虹膜认证", Toast.LENGTH_SHORT).show();
					}*/
				}
				break;

				case 2://fp retry
				{
					Toast.makeText(MainActivity.this,"请重新录入指纹", Toast.LENGTH_SHORT).show();
				}
				break;

				case 3: //fp failed
				{
					isFPCheckOK=false;
					System.out.println("Fail!");
					/*if(Authentication_level==2)
					{
						Toast.makeText(MainActivity.this,"指纹认证失效，请使用PIN码认证", Toast.LENGTH_SHORT).show();
					}
					else if(Authentication_level==6)
					{
						Toast.makeText(MainActivity.this,"指纹认证失效，无法解锁，请联系管理员", Toast.LENGTH_SHORT).show();
					}
					else if(Authentication_level==7)
					{
						Toast.makeText(MainActivity.this,"指纹认证失效，无法解锁，请联系管理员", Toast.LENGTH_SHORT).show();
					}
					fp_status=false;*/
				}
				default:
					break;
			}
		}
	};
	//add by yumingyuan finger print完成能够通过
}
