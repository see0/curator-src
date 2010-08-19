<?php
require_once('helperfunctions.php');

function tokens_to_table($record, $float="left") {
	$text = $record->rawText;
	$result = "<table style=\"float: ".$float."\" cellpadding=0 cellspacing=0><tr><td style=\"height: 16px\">&nbsp;</td></tr>";
	//$table[0][$col] = "<td></td>";
	foreach($record->labelViews["tokens"]->labels as $i => $span) {
		$result .= "<tr><td style=\"height: 16px;\" class=\"spacing\">".my_substr($text, $span->start, $span->ending - $span->start)."</td></tr>";
		//$table[$i+1][$col] = "<td>".substr($text, $span->start, $span->ending - $span->start)."</td>";
	}
	$result .= "</table>";
	return $result;
}

function srl_to_table($record, $table, $col = 0) {
	$text = $record->rawText;
	$forest = $record->parseViews["srl"];
	$start_idx = array();
	$ends_idx = array();
	$result = "";
    $tokens = $record->labelViews["tokens"]

	// reverse the maps of the start and end indexes,
	// these map to the word indices
	foreach($tokens->labels as $i => $span) {
		// fix the punctuation problem. (spans like ',' and '.' occupy the space before, instead of space after)
		$ss = $span->start;
		if($i > 0 && $tokens->labels[$i-1]->end == $span->start) {
			$ss = $span->start + 1;
		}
		$start_idx[$ss] = $i;
		// for end positions, there seems to be a problem with text containing '--',
		// so we'll generate the corresponding word index for each character position
		for($j = $ss; $j <= $span->ending; $j++) {
			$ends_idx[$j] = $i;
		}
	}
	$treeobj = array();
	$inspan = array();

	// build our forest of column data ...
	// treeobj[ tree_id ][ start_word_id ] = span, span is an array of 'text', 'class', 'span' => Text visible, CSS class, # of rows spanned
	foreach($forest->trees as $i => $tree) {
		$nodes = $tree->nodes;
		$top = $nodes[$tree->top];
        $predicatename = $top->attributes["predicate"];
		$start = $top->span->start;
		$end = $top->span->ending;

		$verb_rowspan = $ends_idx[$end] - $start_idx[$start] + 1;

		$treeobj[$i][$start_idx[$start]] = array('text' => "<a href=\"srl-frame.php?verb=".$predicatename."\" onclick=\"window.open(this.href, 'srlframe', 'width=400,height=400,scrollbars=yes,resizable=yes'); return false;\">V: ".$predicatename."</a>", 'class' => 'V', 'span' => 1);
		$roles = predict_roles($predicatename);
		foreach($top->children as $child => $label) {
			$child_node = $nodes[$child];
			$start = $child_node->span->start;
			$end = $child_node->span->ending;

			$rowspan = $ends_idx[$end] - $start_idx[$start] + 1;
			$cl = $label;
			if(substr($cl, 0, 1) == "R" || substr($cl, 0, 1) == "C") {
				$lookup_label = substr($cl, 2);
				$cl = $lookup_label;
			} else {
				$lookup_label = $label;
			}
			if(substr($cl, 0, 2) == "AM") {
				$cl = "AM";
			}
			for($j = $start_idx[$start]; $j < $start_idx[$start]+$rowspan; $j++) {
				$span_class[$i][$j] = array('class' => $cl, 'id' => $start);
			}

			$txt = $roles[$lookup_label];
			if($rowspan > 1) {
				$txt = "<a href=\"srl-frame.php?verb=".$predicatename."\" onclick=\"window.open(this.href, 'srlframe', 'width=400,height=400,scrollbars=yes,resizable=yes'); return false;\">".wordwrap($txt, strlen($txt)/1.5, "<br />")." [".$label."]</a>";
			} else {
				$txt = "<a href=\"srl-frame.php?verb=".$predicatename."\" onclick=\"window.open(this.href, 'srlframe', 'width=400,height=400,scrollbars=yes,resizable=yes'); return false;\">".$txt." [".$label."]</a>";
			}
			$treeobj[$i][$start_idx[$start]] = array('text' => $txt, 'class' => $cl, 'span' => $rowspan);
		}
	}


	foreach($treeobj as $i => $tree) {
		$table[0][$col+$i] = "<td class=\"spacing\"><img id=\"srl$i\" onclick=\"toggle_col('srl$i');\" src=\"/images/collapse.gif\" /></td><td col=\"srl$i\" class=\"title\"><i>SRL</i></td>";
		$inspan = 0;
		foreach($tokens->labels as $j => $span) {
			$table[$j+1][$col+$i] = "<td class=\"spacing\"></td>";
			if($inspan > 0) {
				// in span, don't output a TD tag
				$inspan--;
				continue;
			} else if(array_key_exists($j, $tree)) {
				// start of rowspan
				$inspan = $tree[$j]['span'] - 1;
				$table[$j+1][$col+$i] .= "<td col=\"srl$i\" class=\"inspan ".$tree[$j]['class']."\" rowspan=\"".$tree[$j]['span']."\">".$tree[$j]['text']."</td>";
			} else {
				// nothing to put here
				$table[$j+1][$col+$i] .= "<td col=\"srl$i\"></td>";
			}
		}
	}
	return $table;
}

//TODO: this needs updating to the new quantities labels!!!!
function qty_to_table($record, $table, $col = 0) {
	$text = $record->rawText;
	$qtys = $record->labelViews["quantities"];
    $tokens = $record->labelViews["tokens"];
	$start_idx = array();
	$ends_idx = array();
	$result = "";
	$lbls = array('NUM' => "Numerical Phrase", 'TEMP' => "Temporal Phrase");

	// reverse the maps of the start and end indexes,
	// these map to the word indices
	foreach($tokens->labels as $i => $span) {
		// fix the punctuation problem. (spans like ',' and '.' occupy the space before, instead of space after)
		$ss = $span->start;
		if($i > 0 && $tokens->labels[$i-1]->ending == $span->start) {
			$ss = $span->start + 1;
		}
		$start_idx[$ss] = $i;
		// for end positions, there seems to be a problem with text containing '--',
		// so we'll generate the corresponding word index for each character position
		for($j = $ss; $j <= $span->ending; $j++) {
			$ends_idx[$j] = $i;
		}
	}

	// build a much easier quantity object
	// qtyobj[ tree_id ][ start_word_id ] = span, span is an array of 'text', 'class', 'span' => Text visible, CSS class, # of rows spanned
	$qtyobj = array();
	foreach($qtys->labels as $i => $span) {
		$qtyobj[$start_idx[$span->start]] = array('text' => $lbls[$span->label], 'class' => $span->label, 'span' => $ends_idx[$span->ending] - $start_idx[$span->start] + 1);
	}

	$inspan = 0;
	$table[0][$col] = "<td class=\"spacing\"><img id=\"qty\" onclick=\"toggle_col('qty');\" src=\"/images/collapse.gif\" /></td><td col=\"qty\" class=\"qty title\"><i>Quantities</i></td>";
	foreach($tokens->labels as $j => $span) {
		$table[$j+1][$col] = "<td class=\"spacing\"></td>";
		if($inspan > 0) {
			// in span, don't output a TD tag
			$inspan--;
			continue;
		} else if(array_key_exists($j, $qtyobj)) {
			// start of rowspan
			$inspan = $qtyobj[$j]['span'] - 1;
			$table[$j+1][$col] .= "<td col=\"qty\" class=\"qty inspan ".$qtyobj[$j]['class']."\" rowspan=\"".$qtyobj[$j]['span']."\">".$qtyobj[$j]['text']."</td>";
		} else {
			// nothing to put here
			$table[$j+1][$col] .= "<td col=\"qty\" class=\"qty\"></td>";
		}
	}
	return $table;
}

function ner_to_table($record, $table, $col = 0) {
	$text = $record->rawText;
	$ner = $record->labelViews["ner"];
    $tokens = $record->labelViews["tokens"];
	$start_idx = array();
	$ends_idx = array();
	$result = "";
	$lbls = array('PER' => "Person", 'ORG' => "Organization", 'LOC' => "Location", 'MISC' => "Miscellaneous");

	// reverse the maps of the start and end indexes,
	// these map to the word indices
	foreach($tokens->labels as $i => $span) {
		// fix the punctuation problem. (spans like ',' and '.' occupy the space before, instead of space after)
		$ss = $span->start;
		if($i > 0 && $tokens->labels[$i-1]->ending == $span->start) {
			$ss = $span->start + 1;
		}
		$start_idx[$ss] = $i;
		// for end positions, there seems to be a problem with text containing '--',
		// so we'll generate the corresponding word index for each character position
		for($j = $ss; $j <= $span->ending; $j++) {
			$ends_idx[$j] = $i;
		}
	}

	$obj = array();
	foreach($ner->labels as $i => $span) {
		$obj[$start_idx[$span->start]] = array('text' => $lbls[$span->label], 'class' => "NE".$span->label, 'span' => $ends_idx[$span->ending] - $start_idx[$span->start] + 1);
	}

	$inspan = 0;
	$table[0][$col] = "<td class=\"spacing\"><img id=\"ner\" onclick=\"toggle_col('ner');\" src=\"/images/collapse.gif\" /></td><td col=\"ner\" class=\"ner title\"><i>NE</i></td>";
	foreach($tokens->labels as $j => $span) {
		$table[$j+1][$col] = "<td class=\"spacing\"></td>";
		if($inspan > 0) {
			// in span, don't output a TD tag
			$inspan--;
			continue;
		} else if(array_key_exists($j, $obj)) {
			// start of rowspan
			$inspan = $obj[$j]['span'] - 1;
			$table[$j+1][$col] .= "<td col=\"ner\" class=\"ner inspan ".$obj[$j]['class']."\" rowspan=\"".$obj[$j]['span']."\">".$obj[$j]['text']."</td>";
		} else {
			// nothing to put here
			$table[$j+1][$col] .= "<td col=\"ner\" class=\"ner\"></td>";
		}
	}
	return $table;
}

function nom_to_table($record, $table, $col = 0) {
	$text = $record->rawText;
	$forest = $record->parseViews["nom"];;
    $tokens = $record->labelViews["tokens"];
	$start_idx = array();
	$ends_idx = array();
	$lbls = array('SUP' => 'Support', 'C-SUP' => 'Continued Support');

	// reverse the maps of the start and end indexes,
	// these map to the word indices
	foreach($tokens->labels as $i => $span) {
		// fix the punctuation problem. (spans like ',' and '.' occupy the space before, instead of space after)
		// above happens in some cases?? but not others?? don't know...
		$ss = $span->start;
		//if($i > 0 && $tokens->labels[$i-1]->ending == $span->start) {
		//	$ss = $span->start + 1;
		//}
		$start_idx[$ss] = $i;
		// for end positions, there seems to be a problem with text containing '--',
		// so we'll generate the corresponding word index for each character position
		for($j = $ss; $j <= $span->ending; $j++) {
			$ends_idx[$j] = $i;
		}
	}

	foreach($forest->trees as $i => $tree) {
		$nodes = $tree->nodes;
		$top = $nodes[$tree->top];
		$start = $top->span->start;
		$end = $top->span->ending;
        $predicatename = $top->span->attributes["predicate"];

		$verb_rowspan = $ends_idx[$end] - $start_idx[$start] + 1;

		$treeobj[$i]['title'] = $predicatename;
		$table[$start_idx[$start]+1][$col] = "<td class=\"spacing\"></td><td col=\"nom0\"><a href=\"nb-frame.php?verb=".$predicatename."\" onclick=\"window.open(this.href, 'srlframe', 'width=400,height=400,scrollbars=yes,resizable=yes'); return false;\">".$predicatename."</a></td>";
		foreach($top->children as $child => $label) {
			$child_node = $nodes[$child];
			$start = $child_node->span->start;
			$end = $child_node->span->ending;

			$rowspan = $ends_idx[$end] - $start_idx[$start] + 1;
			$cl = $label;
			$lookup_label = $label;
			if(substr($cl, 0, 1) == "R" || substr($cl, 0, 1) == "C") {
				$cl = substr($cl, 2);
			}
			if(substr($cl, 0, 1) == "A") {
				$cl = substr($cl, 0, 2);
			}
			for($j = $start_idx[$start]; $j < $start_idx[$start]+$rowspan; $j++) {
				$span_class[$i][$j] = array('class' => $cl, 'id' => $start);
			}
			if(array_key_exists($lookup_label, $lbls)) {
				$lookup_label = $lbls[$lookup_label];
			}
			if($rowspan > 1) {
				$txt = "<a href=\"nb-frame.php?verb=".$predicatename."\" onclick=\"window.open(this.href, 'srlframe', 'width=400,height=400,scrollbars=yes,resizable=yes'); return false;\">".$lookup_label."<br />[".$label."]</a>";
			} else {
				$txt = "<a href=\"nb-frame.php?verb=".$predicatename."\" onclick=\"window.open(this.href, 'srlframe', 'width=400,height=400,scrollbars=yes,resizable=yes'); return false;\">".$lookup_label." [".$label."]</a>";
			}
			$treeobj[$i][$start_idx[$start]] = array('text' => $txt, 'class' => $cl, 'start' => $start_idx[$start], 'span' => $rowspan);
		}
	}
	// build the first/header column out where objects don't exist so it will be ready to display
	for($i = 0; $i <= count($tokens->labels); $i++) {
		if(!array_key_exists($col, $table[$i])) {
			$table[$i][$col] = "<td class=\"spacing\"></td><td col=\"nom0\"></td>";
		}
	}
	$table[0][$col] = "<td class=\"spacing\"><img id=\"nom0\" onclick=\"toggle_all('nom');\" src=\"/images/collapse.gif\" /></td><td col=\"nom0\" class=\"title\"><i>Nom</i></td>";


	$col++;
	foreach($treeobj as $i => $tree) {
		$inspan = 0;
		$table[0][$col+$i] = "<td class=\"spacing\"><img id=\"nom".($i+1)."\" onclick=\"toggle_col('nom".($i+1)."');\" src=\"/images/collapse.gif\" /></td><td col=\"nom".($i+1)."\" class=\"title\"><i>".$tree['title']."</i></td>";
		foreach($tokens->labels as $j => $span) {
			$table[$j+1][$col+$i] = "<td class=\"spacing\"></td>";
			if($inspan > 0) {
				// in span, don't output a TD tag
				$inspan--;
				continue;
			} else if(array_key_exists($j, $tree)) {
				// start of rowspan
				$inspan = $tree[$j]['span'] - 1;
				$table[$j+1][$col+$i] .= "<td col=\"nom".($i+1)."\" class=\"inspan ".$tree[$j]['class']."\" rowspan=\"".$tree[$j]['span']."\">".$tree[$j]['text']."</td>";
			} else {
				// nothing to put here
				$table[$j+1][$col+$i] .= "<td col=\"nom".($i+1)."\"></td>";
			}
		}
	}
	return $table;
}

function charniak_to_table($record, $table, $col = 0) {
	$text = $record->rawText;
	$forest = $record->parseViews["charniak"];
    $tokens = $record->labelViews["tokens"];
	$start_idx = array();
	$ends_idx = array();

	// reverse the maps of the start and end indexes,
	// these map to the word indices
	foreach($tokens->labels as $i => $span) {
		$start_idx[$span->start] = $i;
		$ends_idx[$span->ending] = $i;
	}

	foreach($forest->trees as $i => $tree) {
		$nodes = $tree->nodes;
		$top = $nodes[$tree->top];
		$start = $top->span->start;
		$end = $top->span->ending;

		$prefix = array();
		$suffix = array();

		$queue = array($tree->top);

		while(count($queue) > 0) {
			$top = $nodes[array_shift($queue)];
			if(!array_key_exists($start_idx[$top->span->start], $prefix)) {
				$prefix[$start_idx[$top->span->start]] = array();
			}
			if(!array_key_exists($ends_idx[$top->span->ending], $suffix)) {
				$suffix[$ends_idx[$top->span->ending]] = array();
			}

			array_push($prefix[$start_idx[$top->span->start]], $top->label);
			array_unshift($suffix[$ends_idx[$top->span->ending]], ")");

			if(count($top->children) > 0) {
				foreach($top->children as $i => $val) {
					array_push($queue, $i);
				}
			}
		}

		$table[0][$col+$i] = "<td class=\"spacing\"><img id=\"charniak$i\" onclick=\"toggle_col('charniak$i');\" src=\"/images/collapse.gif\" /></td><td col=\"charniak$i\" class=\"title\"><i>Charniak</i></td>";
		$padding = 0;
		$padding_stack = array();
		foreach($tokens->labels as $j => $span) {
			if(!array_key_exists($j, $prefix)) {
				$table[$j+1][$col+$i] = "<td class=\"spacing\"></td><td col=\"charniak$i\" class=\"charniak\"></td>";
				continue;
			}
			$val = str_repeat("&nbsp;", $padding);
			for($k = 0; $k < count($prefix[$j]); $k++) {
				$val .= "(".$prefix[$j][$k]." ";

				$padding += my_strlen($prefix[$j][$k]) + 2;
				array_push($padding_stack, my_strlen($prefix[$j][$k]) + 2);
			}
			$val .= my_substr($text, $span->start, $span->ending - $span->start);
			$val .= str_repeat(")", count($suffix[$j]));
			for($k = 0; $k < count($suffix[$j]); $k++) {
				$padding -= array_pop($padding_stack);
			}

			$table[$j+1][$col+$i] = "<td class=\"spacing\"></td>";
			$table[$j+1][$col+$i] .= "<td col=\"charniak$i\" class=\"charniak\">".$val."</td>";
		}
	}
	return $table;
}

?>
