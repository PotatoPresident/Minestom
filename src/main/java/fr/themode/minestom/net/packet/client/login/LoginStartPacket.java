package fr.themode.minestom.net.packet.client.login;

import fr.adamaq01.ozao.net.Buffer;
import fr.themode.minestom.entity.GameMode;
import fr.themode.minestom.net.ConnectionManager;
import fr.themode.minestom.net.ConnectionState;
import fr.themode.minestom.net.packet.client.ClientPreplayPacket;
import fr.themode.minestom.net.packet.server.login.JoinGamePacket;
import fr.themode.minestom.net.packet.server.login.LoginSuccessPacket;
import fr.themode.minestom.net.packet.server.play.SpawnPositionPacket;
import fr.themode.minestom.net.player.PlayerConnection;
import fr.themode.minestom.utils.Utils;
import fr.themode.minestom.world.Dimension;

public class LoginStartPacket implements ClientPreplayPacket {

    private String username;

    @Override
    public void process(PlayerConnection connection, ConnectionManager connectionManager) {
        // TODO send encryption request OR directly login success
        LoginSuccessPacket successPacket = new LoginSuccessPacket(username);
        connection.sendPacket(successPacket);

        connection.setConnectionState(ConnectionState.PLAY);
        connectionManager.createPlayer(connection);

        // TODO complete login sequence with optionals packets
        JoinGamePacket joinGamePacket = new JoinGamePacket();
        joinGamePacket.entityId = 32;
        joinGamePacket.gameMode = GameMode.SURVIVAL;
        joinGamePacket.dimension = Dimension.OVERWORLD;
        joinGamePacket.maxPlayers = 0;
        joinGamePacket.levelType = "default";
        joinGamePacket.reducedDebugInfo = false;

        connection.sendPacket(joinGamePacket);

        // TODO minecraft:brand plugin message

        // TODO send server difficulty

        // TODO player abilities

        SpawnPositionPacket spawnPositionPacket = new SpawnPositionPacket();
        spawnPositionPacket.x = 50;
        spawnPositionPacket.y = 50;
        spawnPositionPacket.z = 50;

        //  connection.sendPacket(spawnPositionPacket);

    }

    @Override
    public void read(Buffer buffer) {
        this.username = Utils.readString(buffer);
    }
}
