import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RankingDisplayComponent } from './components/ranking-display/ranking-display.component';
import { TranslateModule } from '@ngx-translate/core';
import { RankingService } from './services/ranking.service';

/**
 *
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.7.0
 * @export
 * @class RankingModule
 */
@NgModule({
  imports: [
    CommonModule,
    TranslateModule.forChild()
  ],
  declarations: [RankingDisplayComponent],
  providers: [
    RankingService
  ]
})
export class RankingModule {

}
