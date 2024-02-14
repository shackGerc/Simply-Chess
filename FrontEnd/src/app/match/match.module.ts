import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';

import { MatchRoutingModule } from './match-routing.module';
import { MatchComponent } from './components/match/match.component';
import {BoardComponent} from "./components/board/board.component";
import { MatchMenuComponent } from './components/match-menu/match-menu.component';


@NgModule({
  declarations: [
    BoardComponent,
    MatchComponent,
    MatchMenuComponent
  ],
  imports: [
    CommonModule,
    MatchRoutingModule
  ]
})
export class MatchModule { }
