import { Component, OnInit, ViewChild, ElementRef } from '@angular/core';
import { NgForm } from '@angular/forms';

import { AllianceStorage } from '../../storages/alliance.storage';
import { Alliance } from '../../types/alliance.type';
import { ModalComponent } from '../../../../components/modal/modal.component';
import { LoadingService } from '../../../../services/loading.service';
import { AllianceService } from '../../services/alliance.service';

@Component({
  selector: 'app-alliance-of-user',
  templateUrl: './alliance-of-user.component.html',
  styleUrls: ['./alliance-of-user.component.less']
})
export class AllianceOfUserComponent implements OnInit {

  @ViewChild(ModalComponent) public modal: ModalComponent;

  @ViewChild('form') public form: NgForm;

  public userAlliance: Alliance;

  public editingalliance: Alliance;

  constructor(
    private _allianceStorage: AllianceStorage,
    private _loadingService: LoadingService,
    private _allianceService: AllianceService
  ) { }

  public ngOnInit() {
    this._allianceStorage.userAlliance.subscribe(alliance => this.userAlliance = alliance);
  }

  public createEditAlliance(): void {
    this.editingalliance = this.userAlliance
      ? this.userAlliance
      : <any>{};
    this.modal.show();
  }

  public async save(): Promise<void> {
    await this._loadingService.addPromise(this._allianceService.save(this.editingalliance).toPromise());
    this.modal.hide();
  }
}
