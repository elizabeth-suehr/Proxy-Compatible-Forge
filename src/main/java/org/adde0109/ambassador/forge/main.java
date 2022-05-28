package org.adde0109.ambassador.forge;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.netty.buffer.Unpooled;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.status.server.SServerInfoPacket;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ConfigTracker;
import net.minecraftforge.fml.network.FMLHandshakeMessages;
import net.minecraftforge.registries.RegistryManager;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;

import java.util.ArrayList;
import java.util.List;


@Mod("ambassador")
public class main {

  public static int partNrToSend;

  private static final int MAX_DATA_LENGTH = 16000;

  public static List<byte[]> parts = new ArrayList<>();

  public static String packetSplitters;

  public static boolean dataBuilt;

  public main() {
    partNrToSend = 1;
    packetSplitters = "";
    dataBuilt = false;
  }


  public static void buildData() {
    FMLHandshakeMessages.S2CModList s2CModList = new FMLHandshakeMessages.S2CModList();
    List<Pair<String, FMLHandshakeMessages.S2CRegistry>> registryPackets = RegistryManager.generateRegistryPackets(false);
    List<Pair<String, FMLHandshakeMessages.S2CConfigData>> configPackets = ConfigTracker.INSTANCE.syncConfigs(false);


    PacketBuffer buffer = new PacketBuffer(Unpooled.buffer());


    //Mod List
    packetSplitters += ":" + Integer.toString(buffer.writerIndex());

    buffer.writeResourceLocation(new ResourceLocation("fml:handshake"));

    int index = buffer.writerIndex();
    LogManager.getLogger().warn("Length is at index: " + String.valueOf(index));
    buffer.writeVarInt(calculateLength(s2CModList));
    LogManager.getLogger().warn("It should be: " + String.valueOf(buffer.getByte(index)));
    LogManager.getLogger().warn("It should be: " + String.valueOf(buffer.getByte(index+1)));
    LogManager.getLogger().warn("It should be: " + String.valueOf(buffer.getByte(index+2)));
    LogManager.getLogger().warn("It should be: " + String.valueOf(buffer.getByte(index+3)));
    buffer.writeVarInt(1);
    s2CModList.encode(buffer);


    //Registries
    for (Pair<String, FMLHandshakeMessages.S2CRegistry> registryPacket : registryPackets) {
      packetSplitters += ":" + Integer.toString(buffer.writerIndex());

      FMLHandshakeMessages.S2CRegistry registry = registryPacket.getRight();

      buffer.writeResourceLocation(new ResourceLocation("fml:handshake"));
      buffer.writeVarInt(calculateLength(registry));
      buffer.writeVarInt(3);
      encode(registry, buffer);
    }

    //Configs
    for (Pair<String, FMLHandshakeMessages.S2CConfigData> configPacket : configPackets) {
      packetSplitters += ":" + Integer.toString(buffer.writerIndex());

      FMLHandshakeMessages.S2CConfigData config = configPacket.getRight();

      buffer.writeResourceLocation(new ResourceLocation("fml:handshake"));
      buffer.writeVarInt(calculateLength(config));
      buffer.writeVarInt(4);
      encode(config, buffer);
    }



    //Place everything into an array
    //Splice into parts to fit a statusResponse
    while (buffer.readableBytes() > 0) {
      byte[] data = new byte[Math.min(buffer.readableBytes(), MAX_DATA_LENGTH)];
      buffer.readBytes(data);
      parts.add(data);
    }
    buffer.release();
    dataBuilt = true;
  }






  private static int calculateLength(FMLHandshakeMessages.S2CModList s2CModList) {
    PacketBuffer buffer = new PacketBuffer(Unpooled.buffer());
    buffer.writeVarInt(1);
    s2CModList.encode(buffer);
    int length = buffer.writerIndex();
    buffer.release();
    return length;
  }

  private static int calculateLength(FMLHandshakeMessages.S2CRegistry s2CRegistry) {
    PacketBuffer buffer = new PacketBuffer(Unpooled.buffer());
    buffer.writeVarInt(2);
    encode(s2CRegistry,buffer);
    int length = buffer.writerIndex();
    buffer.release();
    return length;
  }

  private static int calculateLength(FMLHandshakeMessages.S2CConfigData s2CConfigData) {
    PacketBuffer buffer = new PacketBuffer(Unpooled.buffer());
    buffer.writeVarInt(2);
    encode(s2CConfigData,buffer);
    int length = buffer.writerIndex();
    buffer.release();
    return length;
  }

  private static void encode(FMLHandshakeMessages.S2CRegistry config, PacketBuffer buffer) {
    buffer.writeResourceLocation(config.getRegistryName());
    buffer.writeBoolean(config.hasSnapshot());
    if (config.hasSnapshot())
      buffer.writeBytes(config.getSnapshot().getPacketData());
  }

  private static void encode(FMLHandshakeMessages.S2CConfigData config, PacketBuffer buffer) {
    buffer.writeUtf(config.getFileName());
    buffer.writeByteArray(config.getBytes());
  }


  private static int calculateAvailableSize(JsonObject jsonObject) {
    jsonObject.add("modinfo",serializeJson("0","1-20:0:32325:64574:99879:---------------------"));
    int size = SServerInfoPacket.GSON.toJson(jsonObject).length();
    jsonObject.remove("modinfo");
    return 32767-size;
  }


  public static JsonObject serializeJson(String data,String version) {
    JsonObject modinfo = new JsonObject();
    JsonArray modList = new JsonArray();

    JsonObject mod = new JsonObject();

    mod.addProperty("modid",data);
    mod.addProperty("version", version);

    modList.add(mod);

    modinfo.addProperty("type", "ambassador");
    modinfo.add("modList",modList);

    return modinfo;

  }

}


