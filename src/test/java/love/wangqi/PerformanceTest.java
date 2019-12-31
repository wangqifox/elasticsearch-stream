package love.wangqi;

import com.fasterxml.jackson.databind.ObjectMapper;
import love.wangqi.stream.InputStreamStreamInput;
import love.wangqi.stream.OutputStreamStreamOutput;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * @author: wangqi
 * @description:
 * @Version:
 * @date: Created in 2019/12/31 3:32 下午
 */
public class PerformanceTest {
    Random random = new Random();
    int loop = 1000;
    int featureSize = 1024;
    User user;

    @Before
    public void before() {
        user = new User();
        user.setId(1);
        user.setName("this is my name");
        user.setAge((short) 20);
        user.setFeature(IntStream.range(0, featureSize)
                .mapToDouble(i -> random.nextDouble()).boxed().collect(Collectors.toList()));
    }

    @Test
    public void writeStreamToBytes() throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        OutputStreamStreamOutput outputStreamStreamOutput = new OutputStreamStreamOutput(byteArrayOutputStream);

        long start = System.currentTimeMillis();
        for (int i = 0; i < loop; i++) {
            user.writeTo(outputStreamStreamOutput);
            outputStreamStreamOutput.flush();
        }
        long end = System.currentTimeMillis();
        System.out.println("cost: " + (end - start));

        int size = byteArrayOutputStream.size();
        System.out.println(size);

        outputStreamStreamOutput.close();
    }

    @Test
    public void writeJsonToBytes() throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        ObjectMapper mapper = new ObjectMapper();

        long start = System.currentTimeMillis();
        for (int i = 0; i < loop; i++) {
            mapper.writeValue(byteArrayOutputStream, user);
        }
        long end = System.currentTimeMillis();
        System.out.println("cost: " + (end - start));

        int size = byteArrayOutputStream.size();
        System.out.println(size);

        byteArrayOutputStream.close();
    }

    @Test
    public void readStreamFromBytes() throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        OutputStreamStreamOutput outputStreamStreamOutput = new OutputStreamStreamOutput(byteArrayOutputStream);

        user.writeTo(outputStreamStreamOutput);
        outputStreamStreamOutput.flush();
        outputStreamStreamOutput.close();

        byte[] bytes = byteArrayOutputStream.toByteArray();

        long start = System.currentTimeMillis();
        for (int i = 0; i < loop; i++) {
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes);
            InputStreamStreamInput inputStreamStreamInput = new InputStreamStreamInput(byteArrayInputStream);
            User newUser = new User();
            newUser.readFrom(inputStreamStreamInput);
        }
        long end = System.currentTimeMillis();
        System.out.println("cost: " + (end - start));
    }

    @Test
    public void readJsonFromBytes() throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        ObjectMapper mapper = new ObjectMapper();
        mapper.writeValue(byteArrayOutputStream, user);

        byte[] bytes = byteArrayOutputStream.toByteArray();

        long start = System.currentTimeMillis();
        for (int i = 0; i < loop; i++) {
            User newUser = mapper.readValue(bytes, User.class);
        }
        long end = System.currentTimeMillis();
        System.out.println("cost: " + (end - start));
    }
}
