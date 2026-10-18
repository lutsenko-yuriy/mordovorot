package web

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Out takes 100 ms and in 200 ms, so the numbers below read as times. */
private fun feed() = MessageFeed<String>(outMs = 100.0, inMs = 200.0)

class MessageFeedTest {

    @Test
    fun `the first message rises in from below and fades in`() {
        val feed = feed()
        feed.show("hi", now = 0.0)

        assertEquals(MessageFeed.Frame("hi", rise = 1.0, alpha = 0.0), feed.at(0.0))
        val half = feed.at(100.0)
        assertEquals("hi", half.message)
        assertTrue(half.rise in 0.0..0.5, "$half")
        assertEquals(0.5, half.alpha)
        assertEquals(MessageFeed.Frame("hi", rise = 0.0, alpha = 1.0), feed.at(200.0))
        assertFalse(feed.moving(200.0))
    }

    @Test
    fun `a new message waits until the old one has risen out and faded completely`() {
        val feed = feed()
        feed.show("old", now = 0.0)
        feed.show("new", now = 500.0)

        val leaving = feed.at(550.0)
        assertEquals("old", leaving.message)
        assertTrue(leaving.rise < 0, "$leaving")
        assertEquals(0.5, leaving.alpha)
        assertEquals(MessageFeed.Frame("new", rise = 1.0, alpha = 0.0), feed.at(600.0))
        assertEquals(MessageFeed.Frame("new", rise = 0.0, alpha = 1.0), feed.at(800.0))
    }

    @Test
    fun `a running phase finishes first, then the latest message follows`() {
        val feed = feed()
        feed.show("a", now = 0.0)
        feed.show("b", now = 50.0)
        feed.show("c", now = 60.0)

        assertEquals("a", feed.at(150.0).message)
        assertEquals("a", feed.at(250.0).message)
        assertEquals(MessageFeed.Frame("c", rise = 1.0, alpha = 0.0), feed.at(300.0))
        assertEquals(MessageFeed.Frame("c", rise = 0.0, alpha = 1.0), feed.at(500.0))
    }

    @Test
    fun `going silent fades the message out and leaves nothing`() {
        val feed = feed()
        feed.show("hi", now = 0.0)
        feed.show(null, now = 300.0)

        assertEquals(MessageFeed.Frame<String>(null, rise = 0.0, alpha = 0.0), feed.at(400.0))
        assertFalse(feed.moving(400.0))
    }

    @Test
    fun `the same message again changes nothing`() {
        val feed = feed()
        feed.show("hi", now = 0.0)
        feed.show("hi", now = 300.0)

        assertEquals(MessageFeed.Frame("hi", rise = 0.0, alpha = 1.0), feed.at(300.0))
        assertFalse(feed.moving(300.0))
    }

    @Test
    fun `no durations show each message at once`() {
        val feed = MessageFeed<String>(outMs = 0.0, inMs = 0.0)
        feed.show("a", now = 0.0)
        feed.show("b", now = 10.0)

        assertEquals(MessageFeed.Frame("b", rise = 0.0, alpha = 1.0), feed.at(10.0))
    }

    @Test
    fun `a frame stamped before the phase began shows the phase's start`() {
        val feed = feed()
        feed.show("hi", now = 100.0)

        assertEquals(MessageFeed.Frame("hi", rise = 1.0, alpha = 0.0), feed.at(95.0))
    }
}
