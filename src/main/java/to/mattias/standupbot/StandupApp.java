package to.mattias.standupbot;

import static java.lang.String.format;

import com.slack.api.bolt.App;
import com.slack.api.bolt.context.builtin.SlashCommandContext;
import com.slack.api.bolt.jetty.SlackAppServer;
import com.slack.api.bolt.response.Response;
import com.slack.api.methods.SlackApiException;
import com.slack.api.methods.request.chat.ChatPostMessageRequest;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class StandupApp {

  private static List<String> userIds;
  private static List<String> userNames;
  private static int seconds;

  public static void main(String[] args) throws Exception {
    var app = new App();

    app.command("/standup", (req, ctx) -> {
      var token = ctx.getBotToken();
      userIds = new ArrayList<>();
      userNames = new ArrayList<>();
      var text = req.getPayload().getText();
      if (text == null) {
        return sendUsageError(ctx);
      }
      trimUsers(text);
      if (userIds.isEmpty()) {
        return sendUsageError(ctx);
      }
      sendPersonalMessage(app, req.getContext().getChannelId(), token);

      if (seconds == 0) {
        seconds = 120;
      }
      var timer = new Timer(userIds, seconds, app, req.getContext().getChannelId(), token);
      new Thread(timer).start();

      return ctx.ack(format("Starting standup%n%d seconds each", seconds));
    });

    app.command("/standup-seconds", (req, ctx) -> {
      seconds = Integer.parseInt(req.getPayload().getText());
      return ctx.ack(format("Setting the time to %d seconds", seconds));
    });

    var server = new SlackAppServer(app);
    server.start();

  }

  private static Response sendUsageError(SlashCommandContext ctx) {
    return ctx.ack(format("You must tag the participant users%n\"/standup @user1 "
        + "@user2\""));
  }

  private static void trimUsers(String text) {
    String[] ids =
        text.replace("<", "")
            .replace(">", "")
            .replace("@", "")
            .split(" ");

    for (var id : ids) {
      userIds.add(id.substring(0, id.indexOf("|")));
    }
  }

  private static void sendPersonalMessage(App app, String channel, String token) throws SlackApiException,
      IOException {
    StringBuilder sb = new StringBuilder();
    for (String id : userIds) {
      sb.append("<@").append(id).append(">").append("\n");
    }
    sb.append("Standup starts now");
    var request = ChatPostMessageRequest.builder()
        .token(token)
        .channel(channel)
        .text(sb.toString())
        .build();

    app.client().chatPostMessage(request);
  }
}
