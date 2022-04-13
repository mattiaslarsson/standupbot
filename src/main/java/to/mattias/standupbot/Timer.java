package to.mattias.standupbot;

import static java.lang.String.format;

import com.slack.api.bolt.App;
import com.slack.api.methods.request.chat.ChatPostMessageRequest;
import java.util.Collections;
import java.util.List;
import java.util.Stack;

public class Timer implements Runnable {

  private Stack<String> userIds = new Stack<>();
  private int totalSeconds;
  private App app;
  private String channel;
  private String token;
  private int timerIntervalMillis = 10000;

  public Timer(List<String> userIds, int seconds, App app, String channel, String token) {
    userIds.forEach(id -> this.userIds.push(id));
    this.totalSeconds = seconds;
    this.app = app;
    this.channel = channel;
    this.token = token;
  }


  @Override
  public void run() {
    Collections.shuffle(userIds);
    while(!userIds.isEmpty()) {
      var currentUser = userIds.pop();
      sendStartMessage(currentUser);
      var currentTime = totalSeconds;
      try {
        do {
          Thread.sleep(timerIntervalMillis);
          currentTime -= timerIntervalMillis / 1000;
          if (currentTime > 0) {
            sendTimeMessage(currentUser, currentTime);
          }
        } while (currentTime > 0);
        sendStopMessage(currentUser);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }

  }

  private void sendStartMessage(String userId) {
    var timeString = formatTime(totalSeconds);

    var request = ChatPostMessageRequest.builder()
        .token(token)
        .channel(channel)
        .text(format("<@%s> Your turn. You got %s", userId, timeString))
        .build();
    try {
      app.client().chatPostMessage(request);
    } catch (Exception ignored) {

    }
  }

  private void sendTimeMessage(String userId, int timeLeft) {
    var timeString = formatTime(timeLeft);

    var request = ChatPostMessageRequest.builder()
        .token(token)
        .channel(channel)
        .text(format("<@%s> You've got %s left", userId, timeString))
        .build();

    try {
      app.client().chatPostMessage(request);
    } catch (Exception ignored) {

    }
  }

  private void sendStopMessage(String userId) {
    var request = ChatPostMessageRequest.builder()
        .token(token)
        .channel(channel)
        .text(format("<@%s> That's it! Your time is up", userId))
        .build();

    try {
      app.client().chatPostMessage(request);
    } catch (Exception ignored) {

    }
  }

  private String formatTime(int secondsLeft) {
    var minutes = (secondsLeft % 3600) / 60;
    var seconds = secondsLeft % 60;

    return format("%02d minutes and %02d seconds", minutes, seconds);
  }
}
