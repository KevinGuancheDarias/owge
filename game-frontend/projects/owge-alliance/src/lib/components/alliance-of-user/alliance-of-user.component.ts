import { Component, OnInit, ViewChild, ElementRef } from '@angular/core';
import { NgForm } from '@angular/forms';

import { AllianceStorage } from '../../storages/alliance.storage';
import { Alliance } from '../../types/alliance.type';
import { ModalComponent, LoadingService } from '@owge/core';
import { AllianceService } from '../../services/alliance.service';
import { Router } from '@angular/router';

@Component({
  selector: 'owge-alliance-of-user',
  templateUrl: './alliance-of-user.component.html',
  styleUrls: ['./alliance-of-user.component.scss']
})
export class AllianceOfUserComponent implements OnInit {

  @ViewChild(ModalComponent, { static: true }) public modal: ModalComponent;

  @ViewChild('form', { static: false }) public form: NgForm;

  public userAlliance: Alliance;

  public editingalliance: Alliance;

  constructor(
    private _allianceStorage: AllianceStorage,
    private _loadingService: LoadingService,
    private _allianceService: AllianceService,
    private _router: Router
  ) { }

  public ngOnInit() {
    this._allianceStorage.userAlliance.subscribe(alliance => this.userAlliance = alliance);
  }

  public createEditAlliance(): void {
    this.editingalliance = this.userAlliance
      ? { ...this.userAlliance }
      : <any>{};
    this.modal.show();
  }

  public async save(): Promise<void> {
    this.editingalliance = await this._loadingService.addPromise(this._allianceService.save(this.editingalliance).toPromise());
    this.modal.hide();
    this._router.navigate(['/alliance/my']);
  }
}
