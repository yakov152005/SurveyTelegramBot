package utils;

public class Constants {

    public static class Details{
        public static final String TOKEN = "6537799423:AAHgEf9HyP-flUoxJ3678gJcZI3OBXf0yuM";
        public static final String USER_NAME = "MyBotYakovbot";
    }

    public static class SurveyCreation {
        public static final String S1 = "immediately";
        public static final String S2 = "delay";

        public static final int FOR_CREATE_SURVEY = 3;
        public static final int LOW_SURVEY = 2;
        public static final int HIGH_SURVEY = 4;
        public static final int MAX_QUESTIONS = 3;
    }

    public static class ERRORS{
        public static final String ERROR_1 = "You need at least 3 members in the community to create a survey.";
        public static final String ERROR_2 = "A survey is already ongoing. Please wait until it finishes before creating a new one.";
        public static final String ERROR_3 = "Try again";
        public static final String ERROR_4 = "You are already a member of the community.";
        public static final String ERROR_5 = "Please provide 2-4 possible answers for this question, separated by commas.";
        public static final String ERROR_6 = "Please provide between 2 to 4 answers, separated by commas. For example: נמר, חתול, אריה, נחש";
        public static final String ERROR_7 = "Please enter a valid number of minutes.";
        public static final String ERROR_8 = "Unknown action. Please try again.";
        public static final String ERROR_9 = "Error: Survey not found for this chat.";
        public static final String ERROR_10 = "Error Send Message.";
        public static final String ERROR_11 = "Survey not found for chat ID: ";
    }

    public static class TEXT{
        public static final String TEXT_1 = "Survey found for chat ID: ";
        public static final String TEXT_2 = "Your answer has been recorded.";
        public static final String TEXT_3 = "Survey Results:\n";
        public static final String TEXT_4 = "Make a Survey";
        public static final String TEXT_5 = "Welcome to the community!";
        public static final String TEXT_6 = "try_create_survey";
        public static final String TEXT_7 = "Next Question";
        public static final String TEXT_8 = "Done";
        public static final String TEXT_9 = "Please send the next question or choose 'done' to finish.";
        public static final String TEXT_10 = "Immediately";
        public static final String TEXT_11 = "Delay";
        public static final String TEXT_12 = "Exit";
    }

    public static class CallBackData{
        public static final String C1 = "next_question";
        public static final String C2 = "done";
        public static final String C3 = "immediately";
        public static final String C4 = "delay";
    }

    public static class TimerClass{
        public static final int SECOND = 1000;
        public static final int MINUTE = 60;
    }
}
