package program;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.*;

import static utils.Constants.CallBackData.*;
import static utils.Constants.Details.*;
import static utils.Constants.ERRORS.*;
import static utils.Constants.SurveyCreation.*;
import static utils.Constants.TEXT.*;
import static utils.Constants.TimerClass.*;


public class MyTelegramBot extends TelegramLongPollingBot {

    private static Set<Long> communityMembers;
    private final Map<Long, Survey> activeSurveys;
    private final Map<Long, Boolean> waitingForDecision;
    private final Map<Long, Map<Integer, String>> surveyResponses;
    private final Set<Long> respondedUsers;
    private final Map<Integer, Integer> messageIdToQuestionIndexMap;
    private Timer surveyTimer;

    public MyTelegramBot() {
        communityMembers = new HashSet<>();
        this.activeSurveys = new HashMap<>();
        this.waitingForDecision = new HashMap<>();
        this.surveyResponses = new HashMap<>();
        this.respondedUsers = new HashSet<>();
        this.messageIdToQuestionIndexMap = new HashMap<>();
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasCallbackQuery()) {
            handleSurveyResponse(update.getCallbackQuery());
        } else if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();
            surveyCreation(update,messageText,chatId);
        }
    }

    private void surveyCreation(Update update, String messageText, long chatId){
        if (waitingForDecision.getOrDefault(chatId, false)) {
            surveyCreationDecision(chatId, messageText);
        } else if (messageText.equalsIgnoreCase(TEXT_4)) {
            if (communityMembers.size() < FOR_CREATE_SURVEY) {
                sendMessage(chatId, ERROR_1);
            } else if (!activeSurveys.isEmpty()) {
                sendMessage(chatId, ERROR_2);
            } else {
                startSurveyCreation(chatId);
            }
        } else if (messageText.equalsIgnoreCase(TEXT_12)) {
            sendMessage(chatId, "You have exited the menu.");
        } else if (messageText.equalsIgnoreCase("/start") || messageText.equalsIgnoreCase("Hi") || messageText.equalsIgnoreCase("היי")) {
            addToCommunity(update);
        } else if (activeSurveys.containsKey(chatId)) {
            handleSurveyInput(chatId, messageText);
        } else {
            sendMessage(chatId, ERROR_3);
        }
    }

    private void addToCommunity(Update update) {
        long chatId = update.getMessage().getChatId();
        String userName = update.getMessage().getFrom().getUserName();
        String firstName = update.getMessage().getFrom().getFirstName();
        String nameToUse = (userName != null) ? userName : firstName;

        if (communityMembers.contains(chatId)) {
            sendMessage(chatId, ERROR_4);
            return;
        }

        addMemberToCommunity(chatId);
        sendWelcomeMessageWithOptions(chatId);
        notifyCommunity(nameToUse);
    }


    private void handleSurveyInput(long chatId, String messageText) {
        Survey survey = activeSurveys.get(chatId);

        if (messageText.equalsIgnoreCase(C2)) {
            finalizeSurveyCreation(chatId);
            return;
        }

        if (!survey.isExpectingAnswers()) {
            survey.addQuestion(messageText);
            sendMessage(chatId, ERROR_5);
            survey.setExpectingAnswers(true);
        } else {
            String[] answers = messageText.split(",");
            if (answers.length >= LOW_SURVEY && answers.length <= HIGH_SURVEY) {
                survey.addAnswers(Arrays.asList(answers));
                survey.setExpectingAnswers(false);
                if (survey.getQuestions().size() < MAX_QUESTIONS ) {
                    sendNextQuestionOrDone(chatId);
                } else {
                    finalizeSurveyCreation(chatId);
                }
            } else {
                sendMessage(chatId, ERROR_6);
            }
        }
    }

    private void startSurveyCreation(long chatId) {
        Survey survey = new Survey(chatId, chatId);
        activeSurveys.put(chatId, survey);
        System.out.println("Survey created and stored for chat ID: " + chatId);
        sendMessage(chatId, "Let's start creating your survey. Please send the first question.");
    }

    private void finalizeSurveyCreation(long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("Your survey is ready. Would you like to send it immediately or after a delay?");

        InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();
        List<InlineKeyboardButton> rowInline = new ArrayList<>();

        InlineKeyboardButton immediatelyButton = new InlineKeyboardButton();
        immediatelyButton.setText(TEXT_10);
        immediatelyButton.setCallbackData(C3);

        InlineKeyboardButton delayButton = new InlineKeyboardButton();
        delayButton.setText(TEXT_11);
        delayButton.setCallbackData(C4);

        rowInline.add(immediatelyButton);
        rowInline.add(delayButton);
        rowsInline.add(rowInline);
        markupInline.setKeyboard(rowsInline);
        message.setReplyMarkup(markupInline);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }

        waitingForDecision.put(chatId, true);
    }

    private void surveyCreationDecision(long chatId, String decision) {
        switch (decision.toLowerCase()) {
            case S1:
                sendSurvey(activeSurveys.get(chatId));
                sendMessage(chatId, "Your survey has been sent immediately.");
                waitingForDecision.remove(chatId);
                break;
            case S2:
                sendMessage(chatId, "When would you like the survey to be sent? (Insert a number in minutes)");
                waitingForDecision.put(chatId, true);
                break;
            default:
                try {
                    int delay = Integer.parseInt(decision);
                    sendSurveyWithDelay(activeSurveys.get(chatId), delay);
                    sendMessage(chatId, "Your survey will be sent in " + delay + " minutes.");
                    waitingForDecision.remove(chatId);
                } catch (NumberFormatException e) {
                    sendMessage(chatId, ERROR_7);
                }
                break;
        }
    }

    private void sendSurvey(Survey survey) {
        for (Long memberChatId : communityMembers) {
            for (int i = 0; i < survey.getQuestions().size(); i++) {
                SendMessage message = survey.createSurveyMessage(memberChatId, i);
                try {
                    Message sentMessage = execute(message);
                    messageIdToQuestionIndexMap.put(sentMessage.getMessageId(), i);
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }
            }
        }

        surveyTimer = new Timer();
        surveyTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                sendSurveyResults(survey.getCreatorId());
                clearSurveyData();
            }
        }, 5 * DEF1 * SECOND);
    }

    private void clearSurveyData() {
        surveyResponses.clear();
        respondedUsers.clear();
        messageIdToQuestionIndexMap.clear();
    }

    private void sendSurveyWithDelay(Survey survey, int delayMinutes) {
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                sendSurvey(survey);
            }
        }, delayMinutes * DEF1 * SECOND);
    }

    private void handleSurveyResponse(CallbackQuery callbackQuery) {
        String callbackData = callbackQuery.getData();
        long chatId = callbackQuery.getMessage().getChatId();
        int messageId = callbackQuery.getMessage().getMessageId();

        System.out.println("Received callback data: " + callbackData);
        System.out.println("Chat ID: " + chatId);

        if (callbackData.equals(TEXT_6)) {
            if (communityMembers.size() < FOR_CREATE_SURVEY) {
                sendMessage(chatId, ERROR_1);
            } else if (!activeSurveys.isEmpty()) {
                sendMessage(chatId, ERROR_2);
            } else {
                startSurveyCreation(chatId);
                System.out.println("Survey created and stored for chat ID: " + chatId);
            }
        } else if (callbackData.equals(C1) || callbackData.equals(C2) || callbackData.equals(C3) || callbackData.equals(C4)) {
            switch (callbackData) {
                case C1:
                    System.out.println("User chose to add next question.");
                    sendMessage(chatId, TEXT_9);
                    break;
                case C2:
                    System.out.println("User finished creating survey.");
                    finalizeSurveyCreation(chatId);
                    break;
                case C3:
                case C4:
                    System.out.println("User chose to send survey " + (callbackData.equals(C3) ? "immediately" : "with delay") + ".");
                    surveyCreationDecision(chatId, callbackData);
                    break;
                default:
                    sendMessage(chatId, ERROR_8);
                    break;
            }
        } else if (messageIdToQuestionIndexMap.containsKey(messageId)) {
            Survey survey = activeSurveys.get(chatId);


            if (survey == null) {
                System.out.println(ERROR_11 + chatId);
                for (Long memberChatId : activeSurveys.keySet()) {
                    survey = activeSurveys.get(memberChatId);
                    if (survey != null) {
                        System.out.println(TEXT_1 + memberChatId + " instead.");
                        break;
                    }
                }
            }

            if (survey == null) {
                sendMessage(chatId, ERROR_9);
                return;
            }

            System.out.println(TEXT_1 + chatId);

            int questionIndex = messageIdToQuestionIndexMap.get(messageId);
            surveyResponses.computeIfAbsent(chatId, k -> new HashMap<>()).put(questionIndex, callbackData);

            boolean allQuestionsAnswered = true;
            for (Long memberChatId : communityMembers) {
                Map<Integer, String> responses = surveyResponses.get(memberChatId);
                if (responses == null || responses.size() < survey.getQuestions().size()) {
                    allQuestionsAnswered = false;
                    break;
                }
            }

            if (allQuestionsAnswered) {
                sendSurveyResults(survey.getCreatorId());
                if (surveyTimer != null) {
                    surveyTimer.cancel();
                }
                clearSurveyData();
            } else {
                sendMessage(chatId, TEXT_2);
            }
        } else {
            sendMessage(chatId, ERROR_8);
        }
    }


    private void sendSurveyResults(long chatId) {
        Survey survey = activeSurveys.remove(chatId);
        if (survey == null) {
            sendMessage(chatId, ERROR_9);
            return;
        }

        StringBuilder resultsMessage = new StringBuilder(TEXT_3);

        int totalResponses = surveyResponses.size();

        for (int questionIndex = 0; questionIndex < survey.getQuestions().size(); questionIndex++) {
            Map<String, Integer> answerCounts = new HashMap<>();

            for (String answer : survey.getAnswers().get(questionIndex)) {
                answerCounts.put(answer, 0);
            }

            for (Map<Integer, String> userResponses : surveyResponses.values()) {
                String answer = userResponses.get(questionIndex);
                answerCounts.put(answer, answerCounts.getOrDefault(answer, 0) + 1);
            }

            resultsMessage.append("Question ").append(questionIndex + 1).append(": ")
                    .append(survey.getQuestions().get(questionIndex)).append("\n");

            answerCounts.entrySet().stream()
                    .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                    .forEach(entry -> {
                        String answer = entry.getKey();
                        int count = entry.getValue();
                        double percentage = (count * 100.0) / (totalResponses > 0 ? totalResponses : 1);
                        resultsMessage.append(answer).append(": ").append(String.format("%.2f", percentage)).append("%\n");
                    });

            resultsMessage.append("\n");
        }

        sendMessage(chatId, resultsMessage.toString());

        clearSurveyData();
    }


    private void sendWelcomeMessageWithOptions(long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(TEXT_5);

        InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();
        List<InlineKeyboardButton> rowInline = new ArrayList<>();

        InlineKeyboardButton createSurveyButton = new InlineKeyboardButton();
        createSurveyButton.setText(TEXT_4);
        createSurveyButton.setCallbackData(TEXT_6);

        rowInline.add(createSurveyButton);
        rowsInline.add(rowInline);
        markupInline.setKeyboard(rowsInline);
        message.setReplyMarkup(markupInline);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }

        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboard = new ArrayList<>();
        KeyboardRow row = new KeyboardRow();

        row.add(TEXT_4);
        row.add(TEXT_12);

        keyboard.add(row);
        keyboardMarkup.setKeyboard(keyboard);
        keyboardMarkup.setResizeKeyboard(true);
        message.setReplyMarkup(keyboardMarkup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }


    private void sendNextQuestionOrDone(long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(TEXT_9);

        InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();
        List<InlineKeyboardButton> rowInline = new ArrayList<>();

        InlineKeyboardButton nextQuestionButton = new InlineKeyboardButton();
        nextQuestionButton.setText(TEXT_7);
        nextQuestionButton.setCallbackData(C1);

        InlineKeyboardButton doneButton = new InlineKeyboardButton();
        doneButton.setText(TEXT_8);
        doneButton.setCallbackData(C2);

        rowInline.add(nextQuestionButton);
        rowInline.add(doneButton);
        rowsInline.add(rowInline);
        markupInline.setKeyboard(rowsInline);
        message.setReplyMarkup(markupInline);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void notifyCommunity(String newUserName) {
        StringBuilder notification = new StringBuilder();
        notification.append("A new member has joined the community --> ")
                .append(newUserName)
                .append("\n")
                .append("Now the community is appointed --> ")
                .append(communityMembers.size())
                .append(" friends.");

        for (Long memberChatId : communityMembers) {
            sendMessage(memberChatId, notification.toString());
        }
    }

    private void sendMessage(long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            System.out.println(ERROR_10);
            e.printStackTrace();
        }
    }

    private void addMemberToCommunity(long chatId) {
        communityMembers.add(chatId);
    }

    @Override
    public String getBotUsername() {
        return USER_NAME;
    }

    @Override
    public String getBotToken() {
        return TOKEN;
    }
}