import { Component, OnInit } from '@angular/core';
import { LoadingService } from '@owge/core';
import { SystemMessage, SystemMessageService } from '@owge/universe';
import { BaseComponent } from '../../base/base.component';

@Component({
  selector: 'app-system-messages',
  templateUrl: './system-messages.component.html',
  styleUrls: ['./system-messages.component.scss']
})
export class SystemMessagesComponent extends BaseComponent implements OnInit {
  public messages: SystemMessage[];
  constructor(private _service: SystemMessageService) {
    super();
  }

  ngOnInit(): void {
    this._subscriptions.add(this._service.findAll().subscribe(messages => {
      this.messages = messages;
      this._markAsRead();
    }));
  }

  public displayTitle(message: SystemMessage): string {
    return new Date(message.creationDate).toISOString().split('.')[0].replace('T', ' ');
  }

  public displayContent(message: SystemMessage): string {
    return message.content;
  }

  private _markAsRead(): void {
    const unreadMessages = this.messages.filter(message => !message.isRead);
    if (unreadMessages.length) {
      this._loadingService.addPromise(this._service.markAsRead(unreadMessages).toPromise());
    }
  }

}
