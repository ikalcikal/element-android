/*
 * Copyright 2019 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package im.vector.app.features.home.room.detail.timeline.item

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import im.vector.app.R
import im.vector.app.core.extensions.setLeftDrawable
import im.vector.app.core.extensions.setTextOrHide
import im.vector.app.core.extensions.setTextWithColoredPart
import im.vector.app.features.home.AvatarRenderer
import im.vector.app.features.home.room.detail.timeline.MessageColorProvider
import im.vector.app.features.home.room.detail.timeline.TimelineEventController
import org.matrix.android.sdk.api.util.MatrixItem
import timber.log.Timber

@EpoxyModelClass(layout = R.layout.item_timeline_event_base_state)
abstract class CallTileTimelineItem : AbsBaseMessageItem<CallTileTimelineItem.Holder>() {

    override val baseAttributes: AbsBaseMessageItem.Attributes
        get() = attributes

    @EpoxyAttribute
    lateinit var attributes: Attributes

    override fun getViewType() = STUB_ID

    override fun bind(holder: Holder) {
        super.bind(holder)
        holder.endGuideline.updateLayoutParams<RelativeLayout.LayoutParams> {
            this.marginEnd = leftGuideline
        }

        holder.creatorNameView.text = attributes.userOfInterest.getBestName()
        attributes.avatarRenderer.render(attributes.userOfInterest, holder.creatorAvatarView)
        holder.callKindView.setText(attributes.callKind.title)
        holder.callKindView.setLeftDrawable(attributes.callKind.icon)
        if (attributes.callStatus == CallStatus.INVITED && !attributes.informationData.sentByMe) {
            holder.acceptRejectViewGroup.isVisible = true
            holder.acceptView.setOnClickListener {
                Timber.v("On accept call: $attributes.callId ")
            }
            holder.rejectView.setLeftDrawable(R.drawable.ic_call_hangup, R.color.riotx_notice)
            holder.rejectView.setOnClickListener {
                Timber.v("On reject call: $attributes.callId")
            }
            holder.statusView.isVisible = false
            when (attributes.callKind) {
                CallKind.CONFERENCE -> {
                    holder.rejectView.setText(R.string.ignore)
                    holder.acceptView.setText(R.string.join)
                    holder.acceptView.setLeftDrawable(R.drawable.ic_call_audio_small, R.color.riotx_accent)
                }
                CallKind.AUDIO -> {
                    holder.rejectView.setText(R.string.call_notification_reject)
                    holder.acceptView.setText(R.string.call_notification_answer)
                    holder.acceptView.setLeftDrawable(R.drawable.ic_call_audio_small, R.color.riotx_accent)
                }
                CallKind.VIDEO -> {
                    holder.rejectView.setText(R.string.call_notification_reject)
                    holder.acceptView.setText(R.string.call_notification_answer)
                    holder.acceptView.setLeftDrawable(R.drawable.ic_call_video_small, R.color.riotx_accent)
                }
            }
        } else {
            holder.acceptRejectViewGroup.isVisible = false
            holder.statusView.isVisible = true
        }
        holder.statusView.setCallStatus(attributes)
        renderSendState(holder.view, null, holder.failedToSendIndicator)
    }

    private fun TextView.setCallStatus(attributes: Attributes) {
        when (attributes.callStatus) {
            CallStatus.INVITED -> if (attributes.informationData.sentByMe) {
                setText(R.string.call_tile_you_started_call)
            }
            CallStatus.IN_CALL -> setText(R.string.call_tile_in_call)
            CallStatus.REJECTED -> if (attributes.informationData.sentByMe) {
                setTextWithColoredPart(R.string.call_tile_you_declined, R.string.call_tile_call_back)
            } else {
                text = context.getString(R.string.call_tile_other_declined, attributes.userOfInterest.getBestName())
            }
            CallStatus.ENDED -> setText(R.string.call_tile_ended)
        }
    }

    class Holder : AbsBaseMessageItem.Holder(STUB_ID) {
        val acceptView by bind<Button>(R.id.itemCallAcceptView)
        val rejectView by bind<Button>(R.id.itemCallRejectView)
        val acceptRejectViewGroup by bind<ViewGroup>(R.id.itemCallAcceptRejectViewGroup)
        val callKindView by bind<TextView>(R.id.itemCallKindTextView)
        val creatorAvatarView by bind<ImageView>(R.id.itemCallCreatorAvatar)
        val creatorNameView by bind<TextView>(R.id.itemCallCreatorNameTextView)
        val statusView by bind<TextView>(R.id.itemCallStatusTextView)
        val endGuideline by bind<View>(R.id.messageEndGuideline)
        val failedToSendIndicator by bind<ImageView>(R.id.messageFailToSendIndicator)
    }

    companion object {
        private const val STUB_ID = R.id.messageCallStub
    }

    data class Attributes(
            val callId: String,
            val callKind: CallKind,
            val callStatus: CallStatus,
            val userOfInterest: MatrixItem,
            override val informationData: MessageInformationData,
            override val avatarRenderer: AvatarRenderer,
            override val messageColorProvider: MessageColorProvider,
            override val itemLongClickListener: View.OnLongClickListener? = null,
            override val itemClickListener: View.OnClickListener? = null,
            override val reactionPillCallback: TimelineEventController.ReactionPillCallback? = null,
            override val readReceiptsCallback: TimelineEventController.ReadReceiptsCallback? = null,
    ) : AbsBaseMessageItem.Attributes

    enum class CallKind(@DrawableRes val icon: Int, @StringRes val title: Int) {
        VIDEO(R.drawable.ic_call_video_small, R.string.action_video_call),
        AUDIO(R.drawable.ic_call_audio_small, R.string.action_voice_call),
        CONFERENCE(R.drawable.ic_call_conference_small, R.string.conference_call_in_progress)
    }

    enum class CallStatus {
        INVITED,
        IN_CALL,
        REJECTED,
        ENDED
    }
}
