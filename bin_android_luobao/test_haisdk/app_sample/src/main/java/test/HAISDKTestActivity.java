package test; /**
 * 测试的样例工程
 */

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import com.haihuman.nativelib.HaiSDK;
import com.haihuman.nativelib.HaiSDKSurfaceView;
import com.haihuman.nativelib.IAudioProxy;
import com.haihuman.nativelib.ICheckServerAsset;
import com.haihuman.nativelib.IDownloadCheckedServerAsset;
import com.haihuman.nativelib.IPlayChat;
import com.haihuman.nativelib.IPlayMotion;
import com.haihuman.nativelib.IWalk;

import java.io.File;

import utils.HAIUtils;

public class HAISDKTestActivity extends Activity {
    private ViewGroup _renderView;
    private HaiSDKSurfaceView _haiSurface;
    private final LocalAnimEnum _localAnimFiles = new LocalAnimEnum();
    private Button _playAnimButton;
    private Button _createSDK;
    private Button _createWindow;
    private static final int ButtonSize = 300;
    private static int _avatarInstanceID = -1;
    private final String TAG = "HAI_NATIVE_JAVA";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        File externalFilesDir = this.getExternalFilesDir(null);
        HAIUtils.copyFilesFromAssets(this, "ext", externalFilesDir.getAbsolutePath());
        Window window = this.getWindow();
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        super.onCreate(savedInstanceState);
        {
            FrameLayout frameLayout = new FrameLayout(this);
            {
                _renderView = new LinearLayout(this);
                frameLayout.addView(_renderView);
            }
            LinearLayout guiView = new LinearLayout(this);
            guiView.setOrientation(LinearLayout.VERTICAL);
            guiView.setGravity(Gravity.BOTTOM | Gravity.RIGHT);
            {
                {
                    LinearLayout layout = new LinearLayout(this);
                    {
                        layout.setOrientation(LinearLayout.HORIZONTAL);
                        layout.setGravity(Gravity.CENTER);
                        _createSDK = new Button(this);
                        _createSDK.setWidth(ButtonSize / 2);
                        _createSDK.setText("SDK");
                        _createSDK.setOnClickListener(new CreateSDK());
                        layout.addView(_createSDK);
                        layout.setOrientation(LinearLayout.HORIZONTAL);
                        layout.setGravity(Gravity.CENTER);
                        _createWindow = new Button(this);
                        _createWindow.setWidth(ButtonSize / 2);
                        _createWindow.setText("NewWindow");
                        _createWindow.setOnClickListener(new CreateWindow());
                        layout.addView(_createWindow);
                        Button b1 = new Button(this);
                        b1.setWidth(ButtonSize / 2);
                        b1.setText("N/D Avatar");
                        b1.setOnClickListener(new CreateAvatarClick());
                        layout.addView(b1);
                        _playAnimButton = new Button(this);
                        _playAnimButton.setWidth(ButtonSize / 2);
                        _playAnimButton.setOnClickListener(new PlayAnimClick());
                        layout.addView(_playAnimButton);
                        Button _playTextButton = new Button(this);
                        _playTextButton.setWidth(ButtonSize / 2);
                        _playTextButton.setText("TEXT BC");
                        _playTextButton.setOnClickListener(new PlayTextClick());
                        layout.addView(_playTextButton);
                    }
                    guiView.addView(layout);
                }
                {
                    LinearLayout layout = new LinearLayout(this);
                    {
                        layout.setOrientation(LinearLayout.HORIZONTAL);
                        layout.setGravity(Gravity.CENTER);
                        Button b2 = new Button(this);
                        b2.setWidth(ButtonSize / 2);
                        b2.setText("POS CHANGE");
                        b2.setOnClickListener(new PositionClick());
                        Button b3 = new Button(this);
                        b3.setWidth(ButtonSize / 2);
                        b3.setText("BK SWITCH");
                        b3.setOnClickListener(new BackgroundClick());
                        layout.addView(b2);
                        layout.addView(b3);
                    }
                    guiView.addView(layout);
                }
            }
            frameLayout.addView(guiView);
            setContentView(frameLayout);
        }
    }

    @Override
    protected void onDestroy() {
        HaiSDK.Quit();
        super.onDestroy();
    }

    void RefreshAnimUI() {
        File anim = _localAnimFiles.getAnim();
        if (anim != null) {
            _playAnimButton.setText(anim.getName());
        } else {
            _playAnimButton.setText("");
        }
    }

    private final String[] _avatarIDs = {"11200280000314470000000000000000"};
    private int _selectAvatarIndex = 0;

    class CreateSDK implements View.OnClickListener {
        private int _state = 0;

        @Override
        public void onClick(View view) {
            if (_state == 0) {
                File externalFilesDir = view.getContext().getExternalFilesDir(null);
                String host = "https://api.haihuman.com/haigate/api/v1";
                String appid = "3632488854771014";
                String appkey = "SOZAuzMGKlxenM6K52t56IjU";
                String appsecret = "r10QumZ5wOq3AdzpJgp2IH85nmEdR39X";
                HaiSDK.Init(true, externalFilesDir.getAbsolutePath(), host, appkey, appid, appsecret, "hai test user", state -> {
                    if (state) {
                        Log.i(TAG, "sdk init:" + state);
                        HaiSDK.CheckServerAsset(_avatarIDs[_selectAvatarIndex], false, state1 -> {
                            Log.i(TAG, "onCheckServerAssetEnd:FinalState:" + state1);
                            if (state1 == ICheckServerAsset.FinalState.HAS_UPDATE_IGNORABLE.ordinal() || state1 == ICheckServerAsset.FinalState.SHOULD_UPDATE.ordinal()) {
                                HaiSDK.DownloadCheckedServerAsset(new IDownloadCheckedServerAsset() {
                                    @Override
                                    public void onDownloadServerAssetProgress(String downloadVersion, int progressStateID, int currentDownloadedSize, int totalSize) {
                                        Log.i(TAG, "onDownloadServerAssetProgress:" + downloadVersion + "->" + progressStateID);
                                    }

                                    @Override
                                    public void onDownloadServerAsset(int checkEndState) {
                                        Log.i(TAG, "onDownloadServerAsset:FinalState:" + checkEndState);
                                    }
                                });
                            }
                        });
                        _state++;
                        _createSDK.setText("QuitSDK");
                    }
                });
            }
            if (_state == 1) {
                _state++;
                if (_renderView != null) {
                    _renderView.removeView(_haiSurface);
                }
                HaiSDK.Quit();
                _createSDK.setText("SDK");
            }
            if (_state == 2) {
                _state = 0;
            }
        }
    }

    class CreateWindow implements View.OnClickListener {
        private int _state = 0;

        @Override
        public void onClick(View v) {
            if (_state == 0) {
                _haiSurface = new HaiSDKSurfaceView(v.getContext());
                _haiSurface.setOnTouchListener(new ShowClick());
                _renderView.addView(_haiSurface);
                _createWindow.setText("DEL WIN");
            }
            if (_state == 1) {
                _renderView.removeView(_haiSurface);
                _createWindow.setText("NEW WIN");
            }
            _state++;
            if (_state == 2) {
                _state = 0;
            }
        }
    }

    class CreateAvatarClick implements View.OnClickListener {
        private int _state = 0;

        @Override
        public void onClick(View v) {
            if (_state == 0) {
                _avatarInstanceID = HaiSDK.CreateAvatar(_avatarIDs[_selectAvatarIndex], false, new IAudioProxy() {
                    @Override
                    public void onStopAudioPlay() {
                        Log.i(TAG, "onStopAudioPlay");
                    }

                    @Override
                    public void onPauseAudioPlay(boolean pauseFlag) {
                        Log.i(TAG, "onPauseAudioPlay");
                    }

                    @Override
                    public void onPlayAudioBuff(byte[] pcmS16Buff, int numOfChan, int samplePerSecond) {
                        Log.i(TAG, "onPlayAudioBuff:" + samplePerSecond);
                    }

                    @Override
                    public void onPlayAudioFile(String wavPath) {
                        Log.i(TAG, "onPlayAudioFile:" + wavPath);
                    }
                }, state -> {
                    Log.i(TAG, "onCreateAvatarEnd:" + state);
                    if (state) {
                        HaiSDK.AvatarDoIdle(_avatarInstanceID);
                    }
                });
                _localAnimFiles.onShowAnim(_avatarIDs[_selectAvatarIndex]);
            } else if (_state == 1) {
                HaiSDK.AvatarChangeSuit(_avatarInstanceID, 1);
            } else if (_state == 2) {
                HaiSDK.AvatarChangeSuit(_avatarInstanceID, 0);
            } else if (_state == 3) {
                HaiSDK.DestroyAvatar(_avatarInstanceID);
            }
            _state++;
            if (_state == 4) {
                _state = 0;
                _selectAvatarIndex++;
                if (_selectAvatarIndex == _avatarIDs.length) {
                    _selectAvatarIndex = 0;
                }
            }
        }
    }

    class PlayAnimClick implements View.OnClickListener {
        private int _playedMotionID;

        @Override
        public void onClick(View v) {
            File anim = _localAnimFiles.getAnim();
            _localAnimFiles.nextAnim();
            RefreshAnimUI();
            if (anim != null) {
                _playedMotionID = HaiSDK.AvatarPlayLocalClip(_avatarInstanceID, anim.getName(), false, "BASE", motionID -> Log.i(TAG, "PLAY END: " + anim.getName() + "->" + motionID + "->" + _playedMotionID));
            }
        }
    }

    class PlayTextClick implements View.OnClickListener {
        private int _idx = 1;

        @Override
        public void onClick(View v) {
            if (_idx == 0) {
                String text = "start cooking";
                HaiSDK.AvatarPlayCloudBehavior(_avatarInstanceID, text, true, "en", "", new IPlayChat() {
                    @Override
                    public void onChatEnd() {
                        Log.d(TAG, "onSteamingEnd");
                    }

                    @Override
                    public void onTextAnswerCallbackEnd(String textAnswer) {
                        Log.d(TAG, "onTextAnswerCallbackEnd :" + textAnswer);
                    }

                    @Override
                    public void onTagTextCallbackEnd(String tagText) {
                        Log.d(TAG, "onTagTextCallbackEnd :" + tagText);
                    }

                    @Override
                    public void onRichTextCallbackEnd(String richText) {
                        Log.d(TAG, "onRichTextCallbackEnd :" + richText);
                    }
                });
            } else if (_idx == 1) {
                String text = "三坊七巷座落于福建省福州市鼓楼区南后街，总占地约45公顷，是从南后街两旁从北至南依次排列的坊巷总称。三坊七巷自晋代发轫，于唐五代形成，到明清鼎盛，如今古老坊巷风貌基本得以传续。三坊七巷为国内现存规模较大、保护较为完整的历史文化街区，有“中国城市里坊制度活化石”和“中国明清建筑博物馆”的美称。2009年6月10日，三坊七巷历史文化街区获得中华人民共和国文化部、国家文物局批准的“中国十大历史文化名街”荣誉称号。";
                HaiSDK.AvatarPlayCloudBehavior(_avatarInstanceID, text, false, "cn", "", new IPlayChat() {
                    @Override
                    public void onChatEnd() {
                        Log.d(TAG, "onSteamingEnd");
                    }

                    @Override
                    public void onTextAnswerCallbackEnd(String textAnswer) {
                        Log.d(TAG, "onTextAnswerCallbackEnd :" + textAnswer);
                    }

                    @Override
                    public void onTagTextCallbackEnd(String tagText) {
                        Log.d(TAG, "onTagTextCallbackEnd :" + tagText);
                    }

                    @Override
                    public void onRichTextCallbackEnd(String richText) {
                        Log.d(TAG, "onRichTextCallbackEnd :" + richText);
                    }
                });
            } else if (_idx == 2) {
                HaiSDK.AvatarDoListen(_avatarInstanceID);
            }
            _idx++;
            if (_idx == 2) {
                _idx = 0;
            }
        }
    }

    class ShowClick implements View.OnTouchListener {
        private int _playedMotionID;

        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            int action = motionEvent.getAction();
            switch (action & MotionEvent.ACTION_MASK) {
                case MotionEvent.ACTION_DOWN:
                    _playedMotionID = HaiSDK.AvatarInteractionClickDoAnimation(_avatarInstanceID, motionEvent.getX(), motionEvent.getY(), false, motionID -> {
                        Log.i(TAG, "CLICK MOTION PLAY END: " + motionID + "->" + _playedMotionID);
                        HaiSDK.AvatarDoIdle(_avatarInstanceID);
                    });
                    break;
            }
            return true;
        }
    }

    class LocalAnimEnum {
        private File[] _allAnims;
        private int _indexForAllAnims = 0;
        private File[] _allPrefabs;
        private int _indexForAllPrefabs = 0;

        public void onShowAnim(String avatarID) {
            _indexForAllAnims = 0;
            _indexForAllPrefabs = 0;
            File file = new File(HaiSDK.GetAssetPath(avatarID) + "/anim/clip");
            _allAnims = file.listFiles(file1 -> !file1.isDirectory());
            file = new File(HaiSDK.GetAssetPath(avatarID) + "/anim/hidb");
            _allPrefabs = file.listFiles(file12 -> !file12.isDirectory());
            RefreshAnimUI();
        }

        public File getAnim() {
            if (_allAnims != null && _allAnims.length > 0 && _indexForAllAnims < _allAnims.length) {
                return _allAnims[_indexForAllAnims];
            }
            return null;
        }

        public File getPrefab() {
            if (_allPrefabs != null && _allPrefabs.length > 0 && _indexForAllPrefabs < _allPrefabs.length) {
                return _allPrefabs[_indexForAllPrefabs];
            }
            return null;
        }

        public void nextAnim() {
            if (_allAnims != null && _allAnims.length > 0 && _indexForAllAnims < _allAnims.length) {
                _indexForAllAnims++;
                if (_indexForAllAnims == _allAnims.length) {
                    _indexForAllAnims = 0;
                }
            }
        }

        public void nextPrefab() {
            if (_allPrefabs != null && _allPrefabs.length > 0 && _indexForAllPrefabs < _allPrefabs.length) {
                _indexForAllPrefabs++;
                if (_indexForAllPrefabs == _allPrefabs.length) {
                    _indexForAllPrefabs = 0;
                }
            }
        }
    }

    public class BackgroundClick implements View.OnClickListener {
        private int _canvasIndex = 0;

        @Override
        public void onClick(View v) {
            Log.i(TAG, " click canvas test:" + _canvasIndex);
            switch (_canvasIndex) {
                case 0:
                    HaiSDK.SetBackgroundImage(HaiSDK.GetAssetPath("11200150000217160000000000000000") + "/background/blue.jpg");
                    break;
                case 1:
                    HaiSDK.SetBackgroundColor(0, 125, 123, 100);
                    break;
                case 2:
                    HaiSDK.HideBackground();
                    break;
            }
            _canvasIndex++;
            if (_canvasIndex == 3) {
                _canvasIndex = 0;
            }
        }
    }

    public class PositionClick implements View.OnClickListener {
        private int _canvasIndex = 0;
        private float[] _trs = new float[4];

        @Override
        public void onClick(View v) {
            switch (_canvasIndex) {
                case 0:
                    Log.i(TAG, " test avatar_scale_to");
                    _trs = HaiSDK.AvatarGetTransform(_avatarInstanceID);
                    Log.i(TAG, " avatar_get_transformation:->" + _trs[0] + "->" + _trs[1] + "->" + _trs[2] + "->" + _trs[3]);
                    HaiSDK.AvatarScaleTo(_avatarInstanceID, 0.5f);
                    HaiSDK.AvatarTranslateTo(_avatarInstanceID, 0, -1.5f);
                    HaiSDK.AvatarRotateTo(_avatarInstanceID, -30);
                    break;
                case 1:
                    HaiSDK.AvatarMoveBy(_avatarInstanceID, "human_zoulu", 5, 5, new IWalk() {
                        @Override
                        public void onWalkEnd() {
                            Log.i(TAG, " AvatarMoveBy:End");
                            HaiSDK.AvatarPlayLocalClip(_avatarInstanceID, "human_zs_jieshao", true, "BASE", new IPlayMotion() {
                                @Override
                                public void onPlayEnd(int motionID) {
                                }
                            });
                            String text = "一种观点认为，南京车次多是因为在京沪高铁上";
                            HaiSDK.AvatarPlayCloudBehavior(_avatarInstanceID, text, false, "cn", "", new IPlayChat() {
                                @Override
                                public void onChatEnd() {
                                    Log.d(TAG, "onSteamingEnd");
                                }

                                @Override
                                public void onTextAnswerCallbackEnd(String textAnswer) {
                                    Log.d(TAG, "onTextAnswerCallbackEnd :" + textAnswer);
                                }

                                @Override
                                public void onTagTextCallbackEnd(String tagText) {
                                    Log.d(TAG, "onTagTextCallbackEnd :" + tagText);
                                    String text = "杭州有杭州东、杭州南、杭州西。即便是吸收了这么多车次，南京南站的客流量都不如杭州东，甚至加上南京站仍少于杭州东.";
                                    HaiSDK.AvatarPlayCloudBehavior(_avatarInstanceID, text, false, "cn", "", new IPlayChat() {
                                        @Override
                                        public void onChatEnd() {
                                            Log.d(TAG, "onSteamingEnd");
                                        }

                                        @Override
                                        public void onTextAnswerCallbackEnd(String textAnswer) {
                                            Log.d(TAG, "onTextAnswerCallbackEnd :" + textAnswer);
                                        }

                                        @Override
                                        public void onTagTextCallbackEnd(String tagText) {
                                            Log.d(TAG, "onTagTextCallbackEnd :" + tagText);
                                        }

                                        @Override
                                        public void onRichTextCallbackEnd(String richText) {
                                            Log.d(TAG, "onRichTextCallbackEnd :" + richText);
                                        }
                                    });
                                }

                                @Override
                                public void onRichTextCallbackEnd(String richText) {
                                    Log.d(TAG, "onRichTextCallbackEnd :" + richText);
                                }
                            });
                        }
                    });
                    break;
            }
            _canvasIndex++;
            if (_canvasIndex == 2) {
                _canvasIndex = 0;
            }
        }
    }
}