package io.middlepoint.tvsleep.services

import co.touchlab.kermit.Logger
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage


class MessagingService: FirebaseMessagingService() {

  private val logger = Logger.withTag("MessagingService")

  override fun onMessageReceived(message: RemoteMessage) {
    super.onMessageReceived(message)
    logger.d { "onMessageReceived: $message" }
  }

  override fun onNewToken(token: String) {
    super.onNewToken(token)
    logger.d { "onNewToken: $token" }
  }

}