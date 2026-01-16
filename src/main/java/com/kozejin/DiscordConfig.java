package com.kozejin;

public class DiscordConfig {
    private String botToken = "Bot Token Here";
    private String channelId = "111111111111111";
    private String commandChannelId = "222222222222222";
    private String adminRoleId = "333333333333333";
    private boolean enabled = false;
    private String chatTagText = "Linked";
    private boolean showChatTag = true;
    private ChatTagColors chatTagColors = new ChatTagColors();
    private MessageFormat messageFormat = new MessageFormat();

    public static class ChatTagColors {
        private String bracketColor = "#808080";
        private String tagColor = "#5865F2";
        private String usernameColor = "#00FFFF";
        private String messageColor = "#FFFFFF";

        public String getBracketColor() { return bracketColor; }
        public String getTagColor() { return tagColor; }
        public String getUsernameColor() { return usernameColor; }
        public String getMessageColor() { return messageColor; }
    }

    public static class MessageFormat {
        private String serverToDiscord = "**{player}**: {message}";
        private String discordToServer = "[Discord] <{user}> {message}";
        private String joinMessage = "**{player}** joined the server";
        private String leaveMessage = "**{player}** left the server";

        public String getServerToDiscord() { return serverToDiscord; }
        public String getDiscordToServer() { return discordToServer; }
        public String getJoinMessage() { return joinMessage; }
        public String getLeaveMessage() { return leaveMessage; }
    }

    public String getBotToken() { return botToken; }
    public String getChannelId() { return channelId; }
    public String getCommandChannelId() { return commandChannelId; }
    public String getAdminRoleId() { return adminRoleId; }
    public boolean isEnabled() { return enabled; }
    public String getChatTagText() { return chatTagText; }
    public boolean isShowChatTag() { return showChatTag; }
    public ChatTagColors getChatTagColors() { return chatTagColors; }
    public MessageFormat getMessageFormat() { return messageFormat; }
}
