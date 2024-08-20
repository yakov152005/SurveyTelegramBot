package program;

import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class Survey {
    private long creatorId;
    private List<String> questions;
    private List<List<String>> answers;
    private boolean expectingAnswers;

    public Survey(long creatorId){
        this.creatorId = creatorId;
        this.questions = new LinkedList<>();
        this.answers = new LinkedList<>();
        this.expectingAnswers = false;
    }

    public void addQuestion(String question){
        if (questions.size() < 3){
            questions.add(question);
            answers.add(new LinkedList<>());
        }
    }

    public void addAnswers(List<String> possibleAnswers){
        if (!answers.isEmpty() && answers.get(answers.size() - 1).isEmpty()) {
            answers.get(answers.size() - 1).addAll(possibleAnswers);
        }
    }

    public boolean isExpectingAnswers() {
        return expectingAnswers;
    }

    public void setExpectingAnswers(boolean expectingAnswers) {
        this.expectingAnswers = expectingAnswers;
    }

    public List<String> getQuestions() {
        return questions;
    }

    public List<List<String>> getAnswers() {
        return answers;
    }

    public SendMessage createSurveyMessage(long chatId, int questionIndex) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("Question " + (questionIndex + 1) + ": " + questions.get(questionIndex));

        InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();
        List<InlineKeyboardButton> rowInline = new ArrayList<>();

        for (String answer : answers.get(questionIndex)) {
            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText(answer);
            button.setCallbackData(answer);
            rowInline.add(button);
        }
        rowsInline.add(rowInline);
        markupInline.setKeyboard(rowsInline);
        message.setReplyMarkup(markupInline);

        return message;
    }

    public long getCreatorId() {
        return creatorId;
    }

    @Override
    public String toString(){
        StringBuilder surveyText = new StringBuilder();
        for (int i = 0; i < questions.size() ; i++) {
            surveyText.append("Question ")
                    .append(i + 1)
                    .append(": ")
                    .append(questions.get(i))
                    .append("\n");
            List<String> ans = answers.get(i);
            for (int j = 0; j < ans.size(); j++) {
                surveyText.append(" ")
                        .append((char)('A' + j))
                        .append(". ")
                        .append(ans.get(j))
                        .append("\n");
            }
        }
        return surveyText.toString();
    }
}
