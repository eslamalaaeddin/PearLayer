package com.example.mediaplayer.listeners;

public interface MediaStateListener {
    void onPreviousButtonClicked();
    void onNextButtonClicked();
    void onPauseButtonClicked();
    void onPlayButtonClicked();
    void onForwardMediaButtonClicked();
    void onBackwardMediaButtonClicked();
    void onChangeMediaPlayerSpeedClicked();
    void onRepeatMediaImageButtonClicked();
}
