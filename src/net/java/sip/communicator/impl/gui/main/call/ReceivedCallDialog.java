/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.call;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.skin.*;

import org.jitsi.util.*;

/**
 * The dialog created when an incoming call is received.
 *
 * @author Yana Stamcheva
 * @author Adam Netocny
 */
public class ReceivedCallDialog
    extends PreCallDialog
    implements ActionListener,
               CallListener,
               Skinnable
{
    /**
     * The incoming call to render.
     */
    private final Call incomingCall;

    /**
     * Creates a <tt>ReceivedCallDialog</tt> by specifying the associated call.
     *
     * @param call The associated with this dialog incoming call.
     * @param video if the call is a video call
     * @param existingCall true to answer the call in an existing call (thus
     * obtaining a conference call)
     */
    public ReceivedCallDialog(Call call, boolean video, boolean existingCall)
    {
        super(GuiActivator.getResources()
            .getSettingsString("service.gui.APPLICATION_NAME")
            + " "
            + GuiActivator.getResources()
                .getI18NString("service.gui.INCOMING_CALL_STATUS")
                    .toLowerCase(), video, existingCall);

        this.incomingCall = call;

        OperationSetBasicTelephony<?> basicTelephony
            = call.getProtocolProvider().getOperationSet(
                    OperationSetBasicTelephony.class);

        basicTelephony.addCallListener(this);

        initCallLabel(getCallLabels());
    }

    /**
     * Initializes the label of the received call.
     *
     * @param callLabel The label to initialize.
     */
    private void initCallLabel(final JLabel callLabel[])
    {
        Iterator<? extends CallPeer> peersIter = incomingCall.getCallPeers();

        boolean hasMorePeers = false;
        String textDisplayName = "";
        String textAddress = "";

        ImageIcon imageIcon =
            ImageUtils.scaleIconWithinBounds(ImageLoader
                .getImage(ImageLoader.DEFAULT_USER_PHOTO), 40, 45);

        while (peersIter.hasNext())
        {
            final CallPeer peer = peersIter.next();

            // More peers.
            if (peersIter.hasNext())
            {
                textDisplayName = callLabel[1].getText()
                    + CallManager.getPeerDisplayName(peer) + ", ";

                String peerAddress = getPeerDisplayAddress(peer);

                if(!StringUtils.isNullOrEmpty(peerAddress))
                    textAddress = callLabel[2].getText()
                        + peerAddress + ", ";

                hasMorePeers = true;
            }
            // Only one peer.
            else
            {
                textDisplayName = callLabel[1].getText()
                    + CallManager.getPeerDisplayName(peer)
                    + " "
                    + GuiActivator.getResources()
                        .getI18NString("service.gui.IS_CALLING");

                String peerAddress = getPeerDisplayAddress(peer);

                if(!StringUtils.isNullOrEmpty(peerAddress))
                    textAddress = callLabel[2].getText()
                        + peerAddress ;

                byte[] image = CallManager.getPeerImage(peer);

                if (image != null && image.length > 0)
                    imageIcon = ImageUtils.getScaledRoundedIcon(image, 50, 50);
                else
                    // Try to find an image in one of the available contact
                    // sources.
                    new Thread(new Runnable()
                    {
                        public void run()
                        {
                            GuiActivator.getContactList()
                                .setSourceContactImage( peer.getAddress(),
                                                        callLabel[0],
                                                        50, 50);
                        }
                    }).start();
            }
        }

        if (hasMorePeers)
            textDisplayName += GuiActivator.getResources()
                .getI18NString("service.gui.ARE_CALLING");

        callLabel[0].setIcon(imageIcon);

        callLabel[1].setText(textDisplayName);

        callLabel[2].setText(textAddress);
        callLabel[2].setForeground(Color.GRAY);
    }

    /**
     * {@inheritDoc}
     *
     * When the <tt>Call</tt> depicted by this dialog is (remotely) ended,
     * close/dispose of this dialog.
     *
     * @param event a <tt>CallEvent</tt> which specifies the <tt>Call</tt> that
     * has ended
     */
    public void callEnded(CallEvent event)
    {
        if (event.getSourceCall().equals(incomingCall))
            dispose();
    }

    @Override
    public void dispose()
    {
        try
        {
            OperationSetBasicTelephony<?> basicTelephony
                = incomingCall.getProtocolProvider().getOperationSet(
                        OperationSetBasicTelephony.class);

            basicTelephony.removeCallListener(this);
        }
        finally
        {
            super.dispose();
        }
    }

    /**
     * Indicates that an incoming call has been received.
     */
    public void incomingCallReceived(CallEvent event) {}

    /**
     * Indicates that an outgoing call has been created.
     */
    public void outgoingCallCreated(CallEvent event) {}

    /**
     * Answers the call when the call button has been pressed.
     */
    @Override
    public void callButtonPressed()
    {
        CallManager.answerCall(incomingCall);
    }

    /**
     * Answers the call in an existing call when the existing call
     * button has been pressed.
     */
    @Override
    public void mergeCallButtonPressed()
    {
        CallManager.answerCallInFirstExistingCall(incomingCall);
    }

    /**
     * Answers the call when the call button has been pressed.
     */
    @Override
    public void videoCallButtonPressed()
    {
        CallManager.answerVideoCall(incomingCall);
    }

    /**
     * Hangups the call when the call button has been pressed.
     */
    @Override
    public void hangupButtonPressed()
    {
        CallManager.hangupCall(incomingCall);
    }

    /**
     * A informative text to show for the peer. If display name and
     * address are the same return null.
     * @param peer the peer.
     * @return the text contain address.
     */
    private String getPeerDisplayAddress(CallPeer peer)
    {
        String peerAddress = peer.getAddress();

        if(StringUtils.isNullOrEmpty(peerAddress, true))
            return null;
        else
        {
            return
                peerAddress.equalsIgnoreCase(peer.getDisplayName())
                    ? null
                    : peerAddress;
        }
    }
}
