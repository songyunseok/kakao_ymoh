package tlschannel;

/**
 * Signals that some IO operation cannot continue because the channel is in non
 * blocking mode and some blocking would otherwise happen.
 */
public class WouldBlockException extends TlsChannelFlowControlException {

}
