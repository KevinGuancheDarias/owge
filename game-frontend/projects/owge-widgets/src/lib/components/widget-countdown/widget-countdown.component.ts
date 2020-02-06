import { Component, OnInit, Input, Output, EventEmitter } from '@angular/core';
import { LoggerHelper, DateRepresentation, DateUtil } from '@owge/core';

/**
 *
 * @deprecated As of 0.8.1 should use the widget version of this class
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @export
 * @class CountdownComponent
 * @implements {OnInit}
 */
@Component({
  selector: 'owge-widgets-countdown',
  templateUrl: './widget-countdown.component.html',
  styleUrls: ['./widget-countdown.component.less']
})
export class WidgetCountdownComponent implements OnInit {

  private intervalID: number;

  /**
   * If should auto start counting defaults to true
   */
  @Input() public autoStart = true;

  @Input() public targetDate: Date;
  @Output() public timeOver: EventEmitter<{}> = new EventEmitter();

  public get done(): boolean {
    return this._done;
  }

  public time: DateRepresentation;

  private _done = false;
  private _log: LoggerHelper = new LoggerHelper(this.constructor.name);


  constructor() { }

  public ngOnInit() {
    this._log.warnDeprecated(this.constructor.name, '0.8.1', 'ng://OwgeWidgets/components/WidgetCountdown');
    if (!(this.targetDate instanceof Date)) {
      throw new Error('targetDate MUST be defined, and MUST be a Date object');
    }

    if (this.autoStart) {
      this.startCounter();
    }
  }

  /**
   * Starts counting only if not already started
   *
   * @author Kevin Guanche Darias
   */
  public startCounter(): void {
    if (!this.intervalID) {
      this.intervalID = window.setInterval(() => this._counterRun(), 1000);
    }
  }

  /**
   * Stops the counter, if running
   *
   * @author Kevin Guanche Darias
   */
  public stopCounter(): void {
    if (this.intervalID) {
      window.clearInterval(this.intervalID);
      this.intervalID = null;
    }
  }

  /**
   * Updates the time fields, sets the counter as done when appropiate
   *
   * @author Kevin Guanche Darias
   */
  private _counterRun(): void {
    const now = new Date();
    const unixTime = new Date(Math.abs(this.targetDate.getTime() - now.getTime()));
    if (now > this.targetDate) {
      this._done = true;
      this.stopCounter();
      this.timeOver.emit();
    } else {
      this.time = DateUtil.milisToDaysHoursMinutesSeconds(unixTime.getTime());
    }

  }
}
