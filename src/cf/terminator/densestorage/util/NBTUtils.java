package cf.terminator.densestorage.util;

import net.minecraft.server.v1_14_R1.NBTBase;

import javax.annotation.Nonnull;
import java.io.ByteArrayOutputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;

public class NBTUtils {
    public static int compare(@Nonnull NBTBase one, @Nonnull NBTBase two){
        int oneID_ = one.getTypeId();
        int twoID_ = two.getTypeId();
        if(oneID_ != twoID_){
            return Integer.compare(oneID_, twoID_);
        }

        ByteArrayOutputStream byteOutOne = new ByteArrayOutputStream();
        ByteArrayOutputStream byteOutTwo = new ByteArrayOutputStream();

        DataOutput outOne = new DataOutputStream(byteOutOne);
        DataOutput outTwo = new DataOutputStream(byteOutTwo);

        try {
            one.write(outOne);
            two.write(outTwo);

            byte[] bytesOne = byteOutOne.toByteArray();
            byte[] bytesTwo = byteOutTwo.toByteArray();

            int i = 0;
            while(i < bytesOne.length && i < bytesTwo.length){
                byte byteOne = bytesOne[i];
                byte byteTwo = bytesTwo[i];
                if(byteOne != byteTwo){
                    return Integer.compare(byteOne, byteTwo);
                }
                i++;
            }
            return Integer.compare(bytesOne.length, bytesTwo.length);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }



    }
}
