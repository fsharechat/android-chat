package cn.wildfire.chat.kit.conversation.message.viewholder;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.text.style.ImageSpan;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.lqr.emoji.MoonUtils;

import butterknife.Bind;
import butterknife.OnClick;
import cn.wildfire.chat.kit.annotation.EnableContextMenu;
import cn.wildfire.chat.kit.annotation.MessageContentType;
import cn.wildfire.chat.kit.annotation.MessageContextMenuItem;
import cn.wildfire.chat.kit.annotation.ReceiveLayoutRes;
import cn.wildfire.chat.kit.annotation.SendLayoutRes;
import cn.wildfire.chat.kit.conversation.message.model.UiMessage;
import cn.wildfirechat.chat.R;
import cn.wildfirechat.message.MediaMessageContent;
import cn.wildfirechat.message.Message;
import cn.wildfirechat.message.MessageContent;
import cn.wildfirechat.message.TextMessageContent;
import cn.wildfirechat.message.core.MessageDirection;
import cn.wildfirechat.message.core.MessageStatus;

@MessageContentType(TextMessageContent.class)
@SendLayoutRes(resId = R.layout.conversation_item_text_send)
@ReceiveLayoutRes(resId = R.layout.conversation_item_text_receive)
@EnableContextMenu
public class TextMessageContentViewHolder extends NormalMessageContentViewHolder {
    @Bind(R.id.contentTextView)
    TextView contentTextView;

    @Bind(R.id.progressBar)
    ProgressBar progressBar;

    public TextMessageContentViewHolder(FragmentActivity activity, RecyclerView.Adapter adapter, View itemView) {
        super(activity, adapter, itemView);
    }

    @Override
    public void onBind(UiMessage message) {
        MoonUtils.identifyFaceExpression(context, contentTextView, ((TextMessageContent) message.message.content).getContent(), ImageSpan.ALIGN_BOTTOM);
        if (message.message.direction == MessageDirection.Receive) {
            progressBar.setVisibility(View.GONE);
        } else {
            // todo
        }
    }

    @OnClick(R.id.contentTextView)
    public void onClickTest(View view) {
        Toast.makeText(context, "onTextMessage click: " + ((TextMessageContent) message.message.content).getContent(), Toast.LENGTH_SHORT).show();
    }


    @MessageContextMenuItem(tag = MessageContextMenuItemTags.TAG_CLIP, title = "复制", confirm = false, priority = 12)
    public void clip(View itemView, UiMessage message) {
        ClipboardManager clipboardManager = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboardManager == null) {
            return;
        }
        TextMessageContent content = (TextMessageContent) message.message.content;
        ClipData clipData = ClipData.newPlainText("messageContent", content.getContent());
        clipboardManager.setPrimaryClip(clipData);
    }

    @Override
    protected void setSendStatus(Message item) {
        super.setSendStatus(item);
        MessageContent msgContent = item.content;
        if (msgContent instanceof TextMessageContent) {
            //只需要设置自己发送的状态
            MessageStatus sentStatus = item.status;
            if (sentStatus == MessageStatus.Sending) {
                progressBar.setVisibility(View.VISIBLE);
            } else if (sentStatus == MessageStatus.Send_Failure) {
                progressBar.setVisibility(View.GONE);
            } else if (sentStatus == MessageStatus.Sent) {
                progressBar.setVisibility(View.GONE);
            }
        }
    }
}
