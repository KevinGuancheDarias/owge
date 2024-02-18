import { Component, OnInit } from '@angular/core';
import { Sponsor } from '@owge/types/core';
import { BaseComponent } from '../../base/base.component';
import { SponsorService } from '../../services/sponsor.service';

/**
 *
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.9.21
 * @export
 */
@Component({
  selector: 'app-sponsors',
  templateUrl: './sponsors.component.html',
  styleUrls: ['./sponsors.component.scss']
})
export class SponsorsComponent extends BaseComponent implements OnInit {

  public activeTab: 'COMPANY' | 'INDIVIDUAL' = 'COMPANY';
  public sponsors: Sponsor[];
  public filteredSponsors: Sponsor[];
  public donatedForCompanies = 20;
  public donatedForIndividuals = 5;
  public donatedForIndividualsWithSite = 10;
  public donationTextTranslation: Record<string, unknown>;

  public constructor(private _sponsorService: SponsorService) {
    super();
    this.donationTextTranslation = {
      donatedForCompanies: this.donatedForCompanies,
      donatedForIndividuals: this.donatedForIndividuals,
      donatedForIndividualsWithSite: this.donatedForIndividualsWithSite
    };
  }

  /**
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @since 0.9.21
   */
  public ngOnInit(): void {
    this._subscriptions.add(this._sponsorService.findAll().subscribe(sponsors => {
      this.sponsors = sponsors;
      this.defineTab('COMPANY');
    }));
  }

  /**
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @since 0.9.21
   * @param tab
   */
  public defineTab(tab: 'COMPANY' | 'INDIVIDUAL'): void {
    this.filteredSponsors = this.sponsors.filter(sponsor => sponsor.type === tab);
    this.activeTab = tab;
  }
}
