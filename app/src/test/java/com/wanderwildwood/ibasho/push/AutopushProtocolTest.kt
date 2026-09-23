package com.wanderwildwood.ibasho.push

import com.google.gson.JsonParser
import com.wanderwildwood.ibasho.push.AutopushProtocol.Client
import com.wanderwildwood.ibasho.push.AutopushProtocol.Server
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AutopushProtocolTest {

    private fun json(text: String) = JsonParser.parseString(text).asJsonObject

    @Test
    fun helloWithoutUaidLeavesItOut() {
        val hello = json(Client.hello(null))
        assertEquals("hello", hello.get("messageType").asString)
        assertFalse(hello.has("uaid"))
    }

    @Test
    fun helloCarriesTheStoredUaid() {
        assertEquals("abc", json(Client.hello("abc")).get("uaid").asString)
    }

    @Test
    fun registerCarriesChannelAndOptionalKey() {
        val plain = json(Client.register("chan", null))
        assertEquals("register", plain.get("messageType").asString)
        assertEquals("chan", plain.get("channelID").asString)
        assertFalse(plain.has("key"))
        assertEquals("vapid", json(Client.register("chan", "vapid")).get("key").asString)
    }

    @Test
    fun ackListsTheUpdate() {
        val ack = json(Client.ack("chan", "v1"))
        assertEquals("ack", ack.get("messageType").asString)
        val update = ack.getAsJsonArray("updates")[0].asJsonObject
        assertEquals("chan", update.get("channelID").asString)
        assertEquals("v1", update.get("version").asString)
    }

    @Test
    fun pingIsAnEmptyObject() {
        assertEquals(0, json(Client.PING).size())
        assertEquals(Server.Ping, Server.parse("{}"))
        assertEquals(Server.Ping, Server.parse("""{"messageType":"ping"}"""))
    }

    @Test
    fun parsesHello() {
        assertEquals(
            Server.Hello("u1", 200),
            Server.parse("""{"messageType":"hello","uaid":"u1","status":200,"use_webpush":true,"broadcasts":{}}""")
        )
    }

    @Test
    fun parsesRegister() {
        assertEquals(
            Server.Register("c1", 200, "https://updates.push.services.mozilla.com/wpush/v1/x"),
            Server.parse("""{"messageType":"register","channelID":"c1","status":200,"pushEndpoint":"https://updates.push.services.mozilla.com/wpush/v1/x"}""")
        )
    }

    @Test
    fun parsesNotificationWithAndWithoutData() {
        assertEquals(
            Server.Notification("c1", "v1", "eA"),
            Server.parse("""{"messageType":"notification","channelID":"c1","version":"v1","data":"eA","headers":{"encoding":"aes128gcm"}}""")
        )
        assertEquals(
            Server.Notification("c1", "v1", null),
            Server.parse("""{"messageType":"notification","channelID":"c1","version":"v1"}""")
        )
    }

    @Test
    fun unknownTypesAreHarmless() {
        assertEquals(Server.Other("broadcast"), Server.parse("""{"messageType":"broadcast","broadcasts":{}}"""))
    }

    @Test
    fun garbageIsNull() {
        assertNull(Server.parse("not json"))
        assertNull(Server.parse("[]"))
        assertNull(Server.parse("""{"status":200}"""))
        assertNull(Server.parse("""{"messageType":"register","status":200}"""))
    }

    @Test
    fun retryDelayGrowsAndLevelsOff() {
        val delays = (1..10).map { AutopushProtocol.retryDelayMillis(it) }
        assertEquals(1_000L, delays.first())
        assertTrue(delays.zipWithNext().all { (a, b) -> b >= a })
        assertEquals(900_000L, delays.last())
    }
}
