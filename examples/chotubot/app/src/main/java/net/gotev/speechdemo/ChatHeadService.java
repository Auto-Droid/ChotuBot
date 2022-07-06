package net.gotev.speechdemo;

import android.app.Service;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import net.gotev.speech.GoogleVoiceTypingDisabledException;
import net.gotev.speech.Speech;
import net.gotev.speech.SpeechDelegate;
import net.gotev.speech.SpeechRecognitionNotAvailable;
import net.gotev.speech.SpeechUtil;
import net.gotev.speech.SupportedLanguagesListener;
import net.gotev.speech.UnsupportedReason;
import net.gotev.speech.ui.SpeechProgressView;

import java.util.List;
import java.util.Locale;

public class ChatHeadService extends Service implements SpeechDelegate {

    private WindowManager mWindowManager;
    private View mChatHeadView;
    SpeechProgressView chatHeadImage;
    StringBuilder textTalk = new StringBuilder();

    public ChatHeadService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        //Inflate the chat head layout we created
        mChatHeadView = LayoutInflater.from(this).inflate(R.layout.chat_head_view, null);

        //Add the view to the window.
        final WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY ,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);

        //Specify the chat head position
        params.gravity = Gravity.TOP | Gravity.LEFT;        //Initially view will be added to top-left corner
        params.x = 0;
        params.y = 100;

        //Add the view to the window
        mWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        mWindowManager.addView(mChatHeadView, params);

        /*//Set the close button.
        ImageView closeButton = (ImageView) mChatHeadView.findViewById(R.id.close_btn);
        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //close the service and remove the chat head from the window
                stopSelf();
            }
        });*/

        //Drag and move chat head using user's touch action.
        chatHeadImage = (SpeechProgressView) mChatHeadView.findViewById(R.id.progress);
        chatHeadImage.setOnTouchListener(new View.OnTouchListener() {
            private int lastAction;
            private int initialX;
            private int initialY;
            private float initialTouchX;
            private float initialTouchY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:

                        //remember the initial position.
                        initialX = params.x;
                        initialY = params.y;

                        //get the touch location
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();

                        lastAction = event.getAction();
                        return true;
                    case MotionEvent.ACTION_UP:
                        //As we implemented on touch listener with ACTION_MOVE,
                        //we have to check if the previous action was ACTION_DOWN
                        //to identify if the user clicked the view or not.
                        if (lastAction == MotionEvent.ACTION_DOWN) {
                            //Open the chat conversation click.
                            Intent intent = new Intent(ChatHeadService.this, MainActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);

                            //close the service and remove the chat heads
                            stopSelf();
                        }
                        lastAction = event.getAction();
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        //Calculate the X and Y coordinates of the view.
                        params.x = initialX + (int) (event.getRawX() - initialTouchX);
                        params.y = initialY + (int) (event.getRawY() - initialTouchY);

                        //Update the layout with new X & Y coordinate
                        mWindowManager.updateViewLayout(mChatHeadView, params);
                        lastAction = event.getAction();
                        return true;
                }
                return false;
            }
        });
        onRecordAudioPermissionGranted(chatHeadImage);
    }

    private void onSetSpeechToTextLanguage() {
        Speech.getInstance().getSupportedSpeechToTextLanguages(new SupportedLanguagesListener() {
            @Override
            public void onSupportedLanguages(List<String> supportedLanguages) {
                CharSequence[] items = new CharSequence[supportedLanguages.size()];
                supportedLanguages.toArray(items);

                new AlertDialog.Builder(ChatHeadService.this)
                        .setTitle("Current language: " + Speech.getInstance().getSpeechToTextLanguage())
                        .setItems(items, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                Locale locale;

                                if (Build.VERSION.SDK_INT >= 21) {
                                    locale = Locale.forLanguageTag(supportedLanguages.get(i));
                                } else {
                                    String[] langParts = supportedLanguages.get(i).split("-");

                                    if (langParts.length >= 2) {
                                        locale = new Locale(langParts[0], langParts[1]);
                                    } else {
                                        locale = new Locale(langParts[0]);
                                    }
                                }

                                Speech.getInstance().setLocale(locale);
                                Toast.makeText(ChatHeadService.this, "Selected: " + items[i], Toast.LENGTH_LONG).show();
                            }
                        })
                        .setPositiveButton("Cancel", null)
                        .create()
                        .show();
            }

            @Override
            public void onNotSupported(UnsupportedReason reason) {
                switch (reason) {
                    case GOOGLE_APP_NOT_FOUND:
                        showSpeechNotSupportedDialog();
                        break;

                    case EMPTY_SUPPORTED_LANGUAGES:
                        new AlertDialog.Builder(ChatHeadService.this)
                                .setTitle(R.string.set_stt_langs)
                                .setMessage(R.string.no_langs)
                                .setPositiveButton("OK", null)
                                .show();
                        break;
                }
            }
        });
    }

    private void showSpeechNotSupportedDialog() {
        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case DialogInterface.BUTTON_POSITIVE:
                        SpeechUtil.redirectUserToGoogleAppOnPlayStore(ChatHeadService.this);
                        break;

                    case DialogInterface.BUTTON_NEGATIVE:
                        break;
                }
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.speech_not_available)
                .setCancelable(false)
                .setPositiveButton(R.string.yes, dialogClickListener)
                .setNegativeButton(R.string.no, dialogClickListener)
                .show();
    }

    @Override
    public void onStartOfSpeech() {
    }

    @Override
    public void onSpeechRmsChanged(float value) {
        //Log.d(getClass().getSimpleName(), "Speech recognition rms is now " + value +  "dB");
    }

    @Override
    public void onSpeechResult(String result) {
        Log.e("talk",""+textTalk.toString());
        boolean isChotuTriggered = false;
        if (result.isEmpty()) {
            Speech.getInstance().say(getString(R.string.repeat));

        } else {
            Speech.getInstance().say(result);
            if(textTalk.toString().contains("Chhotu") || textTalk.toString().contains("Chotu")){
                Speech.getInstance().stopTextToSpeech();
                Intent dialogIntent = new Intent(this, ChatBotWeb.class);
                dialogIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(dialogIntent);
                isChotuTriggered = true;
                stopSelf();
            }
        }

        if(!isChotuTriggered) {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    onRecordAudioPermissionGranted(chatHeadImage);
                }
            }, 10);
        }
    }

    @Override
    public void onSpeechPartialResults(List<String> results) {
        //text.setText("");
        //textTalk.setLength(0);
        for (String partial : results) {
            textTalk.append(" " +partial);
            //Log.e("talk",""+textTalk);
            //text.append(partial + " ");
        }
    }

    private void onRecordAudioPermissionGranted(SpeechProgressView chatHeadImage) {

        try {
            Speech.getInstance().stopTextToSpeech();
            Speech.getInstance().startListening(chatHeadImage, ChatHeadService.this);

        } catch (SpeechRecognitionNotAvailable exc) {
            showSpeechNotSupportedDialog();

        } catch (GoogleVoiceTypingDisabledException exc) {
            //showEnableGoogleVoiceTyping();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mChatHeadView != null) mWindowManager.removeView(mChatHeadView);
    }
}
