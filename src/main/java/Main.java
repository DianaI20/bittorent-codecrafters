import com.dampcake.bencode.Type;
import com.google.gson.Gson;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import com.dampcake.bencode.Bencode;// - available if you need it!

public class Main {
    private static final Gson gson = new Gson();

    public static void main(String[] args) throws Exception {
        // You can use print statements as follows for debugging, they'll be visible when running tests.
        String command = args[0];
        if ("decode".equals(command)) {
            //  Uncomment this block to pass the first stage
            String bencodedValue = args[1];
            Object decoded;
            try {
                decoded = decodeBencode(bencodedValue);
            } catch (RuntimeException e) {
                System.out.println(e.getMessage());
                return;
            }
            System.out.println(gson.toJson(decoded));
        } else {
            System.out.println("Unknown command: " + command);
        }

    }

    static Object decodeBencode(String bencodedString) {
        Bencode bencode =  new Bencode();
        if (Character.isDigit(bencodedString.charAt(0))) {
            return bencode.decode(bencodedString.getBytes(StandardCharsets.UTF_8), Type.STRING);

        }

        if (bencodedString.charAt(0) == 'i' && bencodedString.charAt(bencodedString.length() - 1) == 'e') {
            return bencode.decode(bencodedString.getBytes(StandardCharsets.UTF_8), Type.NUMBER);

        }

        if (bencodedString.charAt(0) == 'l' && bencodedString.charAt(bencodedString.length() - 1) == 'e') {
            return bencode.decode(bencodedString.getBytes(StandardCharsets.UTF_8), Type.LIST);
        }

        return null;
    }

    private static String decodeString (String bencodedString) {
        int firstColonIndex = 0;
        for (int i = 0; i < bencodedString.length(); i++) {
            if (bencodedString.charAt(i) == ':') {
                firstColonIndex = i;
                break;
            }
        }
        int length = Integer.parseInt(bencodedString.substring(0, firstColonIndex));

        return bencodedString.substring(firstColonIndex + 1, firstColonIndex + 1 + length);
    }

    private static Long decodeNumeric(String bencodedString) {
        return Long.parseLong(bencodedString.substring(1, bencodedString.length() - 1));
    }

    private static List<Object> decodeList(String bencodedString) {
        List<Object> list = new ArrayList<>();

        int index = 0;

        if(bencodedString.isEmpty()){
            return list;
        }

        while (index < bencodedString.length()) {

            if (Character.isDigit(bencodedString.charAt(index))) {
                String decodedString = decodeString(bencodedString);
                list.add(decodedString);
                index += decodedString.length() + 2;
            }

            if (bencodedString.charAt(index) == 'i') {
                StringBuilder stringBuilder = new StringBuilder();
                index++;
                while (bencodedString.charAt(index) != 'e'){
                    stringBuilder.append(bencodedString.charAt(index));
                    index++;
                }

                list.add(Long.parseLong(stringBuilder.toString()));
                index++;
            }

            if (bencodedString.charAt(index) == 'l') {
                while (bencodedString.charAt(index) != 'e'){
                  // list.add(decodeList());
                }
            }
        }
        return list;
    }
}
