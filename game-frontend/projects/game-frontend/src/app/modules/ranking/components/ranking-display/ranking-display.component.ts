import { Component, OnInit } from '@angular/core';
import { RankingEntry } from '../../types/ranking-entry';
import { RankingService } from '../../services/ranking.service';

/**
 *
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.7.0
 * @export
 * @class RankingDisplayComponent
 * @implements {OnInit}
 */
@Component({
  selector: 'app-ranking-display',
  templateUrl: './ranking-display.component.html',
  styleUrls: ['./ranking-display.component.less']
})
export class RankingDisplayComponent implements OnInit {

  public entries: RankingEntry[];
  public sorted: RankingEntry[];
  public active: keyof RankingEntry = 'position';

  constructor(private _rankingService: RankingService) {

  }

  async ngOnInit(): Promise<void> {
    this.entries = await this._rankingService.findAll().toPromise();
    this.sorted = this.entries;
  }

  public assignSorted(sorted: RankingEntry[], active: keyof RankingEntry): void {
    this.sorted = sorted;
    this.active = active;
  }

}
