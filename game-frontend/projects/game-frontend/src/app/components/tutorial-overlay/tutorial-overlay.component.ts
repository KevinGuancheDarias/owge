import {
  Component, AfterViewInit, ElementRef, AfterViewChecked, ChangeDetectionStrategy,
  ChangeDetectorRef, ViewChildren, QueryList, ViewChild, ViewEncapsulation
} from '@angular/core';
import { TutorialService } from 'projects/owge-universe/src/lib/services/tutorial.service';
import { TutorialSectionEntry, WebsocketService } from '@owge/universe';
import { ScreenDimensionsService } from '@owge/core';

type decidedPosition = 'top' | 'left' | 'down' | 'right';
interface Position {
  top: number;
  left: number;
  decidedPosition?: decidedPosition;
  width?: number;
  height?: number;
}

/**
 *
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.9.0
 * @export
 */
@Component({
  selector: 'app-tutorial-overlay',
  templateUrl: './tutorial-overlay.component.html',
  styleUrls: ['./tutorial-overlay.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  encapsulation: ViewEncapsulation.None
})
export class TutorialOverlayComponent implements AfterViewInit, AfterViewChecked {

  public runningEntry: TutorialSectionEntry;
  public nodes: HTMLElement[];

  @ViewChild('overlayEl') private _overlayElRef: ElementRef<HTMLDivElement>;
  @ViewChild('textEl') private _textRef: ElementRef<HTMLElement>;
  @ViewChildren('clickBlockerEl') private _clickBlockerRefs: QueryList<ElementRef<HTMLDivElement>>;
  @ViewChildren('imgEl') private _inmgRefs: QueryList<ElementRef<SVGElement>>;

  private _entries: TutorialSectionEntry[];
  private _clickBlockerEls: HTMLDivElement[];
  private _imgEls: SVGElement[];
  private _runCheck = false;
  private _clickOrKeyHandler: any;
  private _domEvents = ['keydown', 'click'];
  private _isPanic = false;
  private _rootEl: HTMLHtmlElement;
  private _isDesktop = false;

  constructor(
    private _tutorialService: TutorialService,
    private _chr: ChangeDetectorRef,
    sds: ScreenDimensionsService,
    websocketService: WebsocketService
  ) {
    websocketService.isCachePanic.subscribe(panic => this._isPanic = panic);
    sds.hasMinWidth(767, 'tutorial_section_overlay').subscribe(has => this._isDesktop = has);
  }

  public ngAfterViewInit(): void {
    this._rootEl = document.querySelector('html');
    this._clickOrKeyHandler = this._defineAsVisited.bind(this);
    this._tutorialService.findApplicableEntries().subscribe(entries => {
      this._entries = entries;
      this._findNextEntry();
    });
  }

  public ngAfterViewChecked(): void {
    if (this.runningEntry && this._runCheck && this.nodes && this.nodes.length) {
      this._clickBlockerEls = this._clickBlockerRefs.toArray().map(ref => ref.nativeElement);
      this._imgEls = this._inmgRefs.toArray().map(ref => ref.nativeElement);
      let position: Position;
      const isFixed = this._isFixed(this.nodes[0]);
      const el: HTMLElement = this._textRef.nativeElement;
      document.body.appendChild(el);
      this.nodes.forEach((node, i) => {
        let currentNodePosition: Position = node.getBoundingClientRect();
        let currentElPosition: Position = el.getBoundingClientRect();
        if (!isFixed) {
          currentNodePosition = this._workaroundScroll(currentNodePosition);
          currentElPosition = this._workaroundScroll(currentElPosition);
        }
        const imgEl: SVGElement = this._imgEls[i];
        if (!position) {
          position = this._calculatePosition(currentNodePosition, currentElPosition);
        }
        if (position) {
          if (i === 0) {
            el.style.top = `${position.top}px`;
            el.style.left = `${position.left}px`;
          }
          if (this.runningEntry.event !== 'CLICK') {
            const div: HTMLDivElement = this._clickBlockerEls[i];
            div.style.top = `${currentNodePosition.top}px`;
            div.style.left = `${currentNodePosition.left}px`;
            div.style.width = `${currentNodePosition.width}px`;
            div.style.height = `${currentNodePosition.height}px`;
            div.style.display = 'block';
          }
          const line: SVGLineElement = document.createElementNS('http://www.w3.org/2000/svg', 'line');
          const svgTarget: Position = this._calculateSvLinePosition(currentNodePosition, position.decidedPosition);
          line.setAttribute('x1', `${position.left + (currentElPosition.width / 2)}px`);
          line.setAttribute('y1', `${position.top + (currentElPosition.height / 2)}px`);
          line.setAttribute('x2', `${svgTarget.left}px`);
          line.setAttribute('y2', `${svgTarget.top}px`);
          // line.setAttribute('stroke', 'red');
          line.setAttribute('stroke-width', '5');
          const rect: SVGRectElement = document.createElementNS('http://www.w3.org/2000/svg', 'rect');
          rect.setAttribute('x', `${currentNodePosition.left - 1}`);
          rect.setAttribute('y', `${currentNodePosition.top - 1}`);
          rect.setAttribute('width', `${currentNodePosition.width + 1}`);
          rect.setAttribute('height', `${currentNodePosition.height + 1}`);
          // rect.setAttribute('stroke', 'red');
          rect.setAttribute('stroke-width', '3');
          rect.setAttribute('fill', 'transparent');
          if (isFixed) {
            this._overlayElRef.nativeElement.style.position = 'fixed';
            this._overlayElRef.nativeElement.style.minHeight = '100%';
            this._textRef.nativeElement.style.position = 'fixed';
          } else {
            this._overlayElRef.nativeElement.style.position = 'absolute';
            this._overlayElRef.nativeElement.style.minHeight = `${this._rootEl.scrollHeight}px`;
            this._textRef.nativeElement.style.position = 'absolute';
          }
          imgEl.appendChild(line);
          imgEl.appendChild(rect);
        }
      });
      let linesAndRects: SVGElement[] = [];
      this._imgEls.forEach(svg => linesAndRects = linesAndRects.concat(Array.from(svg.querySelectorAll('line,rect'))));
      this._runCheck = false;
    }
  }

  private _findNextEntry() {
    if (this.runningEntry && this.nodes) {
      this.nodes.forEach(current => {
        current.style.zIndex = current.attributes['oldZIndex'];
        current.style.position = current.attributes['oldPosition'];
      });
      if (this._textRef && this._textRef.nativeElement && this._clickBlockerEls && this._imgEls) {
        this._textRef.nativeElement.style.display = 'none';
        this._clickBlockerEls.forEach(el => el.style.display = 'none');
        this._imgEls.forEach(svg => svg.style.display = 'none');
      }
    }

    this.runningEntry = this._entries.find(entry => {
      this.nodes = Array.from(document.querySelectorAll(entry.htmlSymbol.identifier));
      return this.nodes.length
        ? this.nodes.some(node => node.offsetParent)
        : false;
    });
    if (this.runningEntry) {
      if (this._textRef) {
        this._textRef.nativeElement.style.display = 'block';
      }
      this.nodes.forEach((current, i) => {
        const computedStyle: CSSStyleDeclaration = window.getComputedStyle(current);
        current.attributes['oldZIndex'] = computedStyle.zIndex;
        current.attributes['oldPosition'] = computedStyle.position;
        current.style.zIndex = '65533';
        if (computedStyle.position === 'static') {
          current.style.position = 'relative';
        }
        if (this.runningEntry.event === 'CLICK') {
          current.addEventListener('click', this._clickOrKeyHandler);
        } else {
          this._domEvents.forEach(event => window.document.addEventListener(event, this._clickOrKeyHandler));
        }

      });
    } else if (this._textRef) {
      document.body.removeChild(this._textRef.nativeElement);
    }
    this._chr.detectChanges();
    this._runCheck = true;

  }

  private _calculatePosition(node: Position, textEl: Position): Position {
    if (this._isDesktop) {
      return this._tryLeft(node, textEl) || this._tryDown(node, textEl)
        || this._tryRight(node, textEl) || this._tryTop(node, textEl);
    } else {
      return this._tryDown(node, textEl) || this._tryTop(node, textEl);
    }
  }

  private _tryLeft(node: Position, el: Position): Position {
    return node.left - el.width - 30 > 0
      ? {
        top: node.top,
        left: node.left - el.width - 30,
        decidedPosition: 'left'
      }
      : null;
  }

  private _tryDown(node: Position, el: Position): Position {
    return node.top + node.height + el.height + 30 < window.innerHeight
      ? {
        top: node.top + node.height + 30,
        left: node.left,
        decidedPosition: 'down'
      }
      : null;
  }

  private _tryRight(node: Position, el: Position): Position {
    return node.left + node.width + el.width + 30 < window.innerWidth
      ? {
        top: node.top,
        left: node.left + node.width + 30,
        decidedPosition: 'right'
      }
      : null;
  }

  private _tryTop(node: Position, el: Position): Position {
    return node.top - el.height - 30 > 0
      ? {
        top: node.top - el.height - 30,
        left: node.left,
        decidedPosition: 'top'
      }
      : null;
  }

  private _calculateSvLinePosition(node: Position, target: decidedPosition): Position {
    switch (target) {
      case 'top':
        return {
          top: node.top,
          left: node.left + (node.width / 2)
        };
      case 'left':
        return {
          top: node.top + (node.height / 2),
          left: node.left
        };
      case 'down':
        return {
          top: node.top + node.height,
          left: node.left + (node.width / 2)
        };
      case 'right':
        return {
          top: node.top + (node.height / 2),
          left: node.left + node.width
        };
    }
  }

  private _defineAsVisited(): void {
    if (!this._isPanic) {
      if (this.runningEntry.event === 'CLICK') {
        this.nodes.forEach(current => current.removeEventListener('click', this._clickOrKeyHandler));
      } else {
        this._domEvents.forEach(event => window.document.removeEventListener(event, this._clickOrKeyHandler));
      }
      this._tutorialService.addVisited(this.runningEntry.id);
    }
  }

  private _workaroundScroll(position: Position): Position {
    return {
      top: position.top + window.scrollY,
      left: position.left + window.scrollX,
      width: position.width,
      height: position.height
    };
  }

  private _isFixed(el: HTMLElement): boolean {
    const parent: HTMLElement = <any>el.offsetParent;
    if (parent && window.getComputedStyle(parent).position === 'fixed') {
      return true;
    } else if (parent.offsetParent && parent.offsetParent.tagName !== 'HTML') {
      return this._isFixed(parent);
    } else {
      return false;
    }
  }
}
