package love.wangqi;

import com.fasterxml.jackson.databind.ObjectMapper;
import love.wangqi.stream.InputStreamStreamInput;
import love.wangqi.stream.OutputStreamStreamOutput;
import org.junit.Test;

import java.io.*;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class RWTest {
    Random random = new Random();

    @Test
    public void writeStreamToBytes() throws IOException {
        User user = new User();
        user.setId(1);
        user.setName("this is my name");
        user.setAge((short) 20);
        user.setFeature(IntStream.range(0, 1024)
                .mapToDouble(i -> random.nextDouble()).boxed().collect(Collectors.toList()));

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        OutputStreamStreamOutput outputStreamStreamOutput = new OutputStreamStreamOutput(byteArrayOutputStream);
        user.writeTo(outputStreamStreamOutput);
        outputStreamStreamOutput.flush();

        int size = byteArrayOutputStream.size();
        System.out.println(size);

        outputStreamStreamOutput.close();
    }

    @Test
    public void writeJsonToBytes() throws IOException {
        User user = new User();
        user.setId(1);
        user.setName("this is my name");
        user.setAge((short) 20);
        user.setFeature(IntStream.range(0, 1024)
                .mapToDouble(i -> random.nextDouble()).boxed().collect(Collectors.toList()));

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        ObjectMapper mapper = new ObjectMapper();
        mapper.writeValue(byteArrayOutputStream, user);

        int size = byteArrayOutputStream.size();
        System.out.println(size);

        byteArrayOutputStream.close();
    }

    @Test
    public void writeStreamToFile() throws IOException {
        User user = new User();
        user.setId(1);
        user.setName("this is my name");
        user.setAge((short) 20);
        user.setFeature(IntStream.range(0, 1024)
                .mapToDouble(i -> random.nextDouble()).boxed().collect(Collectors.toList()));

        File file = new File("/Users/wangqi/Downloads/user");
        FileOutputStream fileOutputStream = new FileOutputStream(file);
        OutputStreamStreamOutput outputStreamStreamOutput = new OutputStreamStreamOutput(fileOutputStream);
        user.writeTo(outputStreamStreamOutput);
        outputStreamStreamOutput.flush();

        outputStreamStreamOutput.close();
        fileOutputStream.close();
    }

    @Test
    public void readStreamFromFile() throws IOException {
        User user = new User();

        File file = new File("/Users/wangqi/Downloads/user");
        FileInputStream fileInputStream = new FileInputStream(file);
        InputStreamStreamInput inputStreamStreamInput = new InputStreamStreamInput(fileInputStream);
        user.readFrom(inputStreamStreamInput);
        System.out.println(user.getId());
        System.out.println(user.getName());
        System.out.println(user.getAge());
        System.out.println(user.getFeature());

        inputStreamStreamInput.close();
        fileInputStream.close();
    }

    @Test
    public void writeJsonToFile() throws IOException {
        User user = new User();
        user.setId(1);
        user.setName("this is my name");
        user.setAge((short) 20);
        user.setFeature(IntStream.range(0, 1024)
                .mapToDouble(i -> random.nextDouble()).boxed().collect(Collectors.toList()));

        File file = new File("/Users/wangqi/Downloads/user.json");
        FileOutputStream fileOutputStream = new FileOutputStream(file);

        ObjectMapper mapper = new ObjectMapper();
        mapper.writeValue(fileOutputStream, user);

        fileOutputStream.close();
    }

}
