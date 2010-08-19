<?php
$GLOBALS['THRIFT_ROOT'] = 'thrift';
require_once $GLOBALS['THRIFT_ROOT'].'/Thrift.php';
require_once $GLOBALS['THRIFT_ROOT'].'/protocol/TBinaryProtocol.php';
require_once $GLOBALS['THRIFT_ROOT'].'/transport/TSocket.php';
require_once $GLOBALS['THRIFT_ROOT'].'/transport/THttpClient.php';
require_once $GLOBALS['THRIFT_ROOT'].'/transport/TBufferedTransport.php';
require_once $GLOBALS['THRIFT_ROOT'].'/transport/TFramedTransport.php';
require_once $GLOBALS['THRIFT_ROOT'].'/packages/curator/Curator.php';

// These my_ functions work out if php has multibyte support and uses
// the mb version if available.
// Note str_replace is multi-byte safe so we don't need a my_ version.

function my_substr($s, $start, $end) {
    if (function_exists('mb_substr')) {
        return mb_substr($s, $start, $end);
    } else {
        return substr($s, $start, $end);
    }
}

function my_strlen($s) {
    if (function_exists('mb_strlen')) {
        return mb_strlen($s);
    } else {
        return strlen($s);
    }
}

function my_reg_replace($pattern, $replacement, $string) {
    if (function_exists('mb_ereg_replace')) {
        return mb_ereg_replace($pattern, $replacement, $string);
    } else {
        return preg_replace($pattern, $replacment, $string);
    }
}



function sanitize_text($text) {
    return str_replace(array("\r\n", "\r", "\n"), "\n", stripslashes($text));
}

function nls2p($str) {
  return str_replace('<p></p>', '', '<p>'
        . my_preg_replace('#([\r\n]\s*?[\r\n]){2,}#', '</p>$0<p>', $str)
        . '</p>');
}

// Check if the label is going to cause problems when used inside a html attribute
function is_bad_label($label) {
    return $label == "#" or $label == '$' or $label == "''" or $label == "``" or $label == "'" or $label == "`" or $label == "," or $label == "." or $label ==":";
}

//builds an array from labels
function build_array($labels) {
    $result = array();
    foreach ($labels as $i => $span) {
        if (!isset($result[$span->start])) {
            $result[$span->start] = array();
        }
        $result[$span->start][$span->end] = $span;
    }
    return $result;
}


function getLabelingHTML($text, $labeling, $name, $newline = false, $offset = 0) {
    if (is_null($labeling)) { return ""; }
    $labels = $labeling->labels;
    $result = '';
    foreach ($labels as $i => $span) {
        $pos = $i+$offset;
        $result .= getSpanHTML($text, $span, $name.$pos);
        if ($newline) $result .= "<br/>";
    }
    return $result;
}

function getSpanHTML($text, $span, $name) {
    $result = "";
    $result .= '<span class="span" id="' . $name. '">';
    if (!is_null($span->label)) {
        if (is_bad_label($span->label)) {
            $result .= '<span class="label PUNC">';
        } else {
            $result .= '<span class="label '.$span->label.'">';
        }
        $result .= htmlspecialchars($span->label);
        $result .= "</span>";
        $result .= ' ';
    }
    if (is_bad_label($span->label)) {
        $result .= '<span class="token PUNC">';
    } else {
        $result .= '<span class="token '.$span->label.'">';
    }
    $result .= htmlspecialchars(my_substr($text, $span->start, $span->ending - $span->start));
    $result .= '</span>';
    if (!is_null($span->attributes)) {
        foreach ($span->attributes as $key => $value) {
            $result .= ' <span class="attribute ' .$span->label.'">'; 
            $result .= htmlspecialchars("[".$key . ": " . $value."]");
            $result .= "</span>&nbsp;";
        }
    }
    $result .= '</span>&nbsp;';
    return $result;
}


function getLabelingJavascript($labeling, $name, $offset=0) {
    if (is_null($labeling)) { return ""; }
    $labels = $labeling->labels;
    $result = "";
    foreach ($labels as $i => $span) {
        $result .= getSpanJavascript($span, $name, $i+$offset);
    }
    return $result;
}

function getSpanJavascript($span, $name, $i) {
    if (is_null($span)) { return  ""; }
    $result = '$("#' . $name . $i . '").click( function() {'. "\n";
    $result .= '$("#text").removeHighlight();' . "\n";
    $result .= '$("#text").highlight(' . "$span->start, $span->ending, 'highlight');\n";
    $result .= "});\n";
    return $result;
}


function getForestHTML($text, $forest, $name, $spans=false) {
    if (is_null($forest)) { return ""; }
    $result = "";
    $counter = 0;

    foreach ($forest->trees as $i => $tree) {
        $topnode = $tree->nodes[$tree->top];
        if (isset($tree->score)) {
            $result .= 'Score: ' .$tree->score .'<br/>';
        }
        $result .= getNodeHTML($text, $topnode, $tree->nodes, 0, $name, $counter, "", true, $spans);
        $result .= "<br/>";
    }
    return $result;
}

function getForestJavascript($forest, $name) {
    if (is_null($forest)) { return ""; }
    $counter = 0;
    $result = "";
    foreach ($forest->trees as $i => $tree) {
        $topnode = $tree->nodes[$tree->top];
        $result .= getNodeJavascript($topnode, $tree->nodes, $name, $counter);
    }
    return $result;

}

function getNodeHTML($text, $node, $nodes, $padding, $name, &$counter, $label="", $first=true, $spans=false) {
    $result = "";
    $dpadding = 0;
    $counter += 1;
    if (!$first) {
        for ($i = 0; $i < $padding; $i++) {
            $result .= "&nbsp;";
        }
    }
    if (!($label == "")) {
        $result .= "<span class='edge'>&lt;$label&gt;</span>&nbsp;";
        $dpadding += my_strlen($label) + 2;
    }
    $span = $node->span;
    if (!($node->label == "dependency node")) {
        $result .= '<span class="label" id="' .$name . $counter. '">'.$node->label.'</span>&nbsp;&nbsp;';
        $dpadding += my_strlen($node->label) + 2;
    }

    if ($spans) {
        $result .= getSpanHTML($text, $span, $name.$counter);
        $result .= '<br/>';
        //$dpadding += $span->ending-$span->start+1;
        $dpadding++;
    }
    if (!is_null($node->children) && !empty($node->children)) {
        ksort($node->children);
        $padding += $dpadding;
        $first = true;
        if ($spans) {
            $first = false;
        }
        foreach ($node->children as $index => $nlabel) {
            $result .= getNodeHTML($text, $nodes[$index], $nodes, $padding, $name, $counter, $nlabel, $first, $spans);
            $first = false;
            
        }
    } else if (!$spans) {
        $result .= getSpanHTML($text, $span, "");
        $result .= '<br/>';
    }
    return $result;
}

function getNodeJavascript($node, $nodes, $name, &$counter) {
    $counter++;
    $result = "";
    $result .= getSpanJavascript($node->span, $name, $counter);
    if (!is_null($node->children) && !empty($node->children)) {
        ksort($node->children);
        foreach ($node->children as $index => $nlabel) {
            $result .= getNodeJavascript($nodes[$index], $nodes, $name, $counter);
        }
    }
    return $result;
}

//this is used in the demos
function getHTMLForLabels($record, $labeling) {
    $labels = build_array($labeling->labels);
    $rawtext = $record->rawText;
    $sents = $record->labelViews["sentences"]->labels;
    ksort($labels);
    $previous = 0;

    $j = 0;
    $result = '<div class="output"><p><span class="sentence">';
    foreach ($labels as $start => $ends) {
        if ($start > $sents[$j]->end) {
            if ($previous < $sents[$j]->end) {
                $result .= htmlspecialchars(my_substr($rawtext, $previous, $sents[$j]->end - $previous));
                $previous = $sents[$j]->end;
            }
            $result .= '</span><span class="sentence">';
            $j = $j + 1;
        }
        ksort($ends);
        if ($start - $previous > 0) {
            $result .= htmlspecialchars(my_substr($rawtext, $previous, $start - $previous));
        }
        foreach ($ends as $end => $span) {
            $result .= getSpanHTML($rawtext, $span, "");
            $previous = $end;
        }

    }
    $result .= htmlspecialchars(my_substr($rawtext, $previous));
    $result .= '</span></p></div>';
    return nl2br($result);
}



// special case for SRL demo

function getSrlHTML($record, $name) {
	$text = $record->rawText;
	$forest = $record->parseViews["srl"];
	$start_idx = array();
	$ends_idx = array();
	$result = "";

	// reverse the maps of the start and end indexes,
	// these map to the word indices
	foreach($record->tokens->labels as $i => $span) {
		$start_idx[$span->start] = $i;
		// for end positions, there seems to be a problem with text containing '--',
		// so we'll generate the corresponding word index for each character position
		for($j = $span->start; $j <= $span->end; $j++) {
			$ends_idx[$j] = $i;
		}
	}
	$treeobj = array();
	$inspan = array();

	foreach($forest->trees as $i => $tree) {
		$nodes = $tree->nodes;
		$top = $nodes[$tree->top];
		$start = $top->span->start;
		$end = $top->span->end;

		$verb_rowspan = $ends_idx[$end] - $start_idx[$start] + 1;

		$treeobj[$i][$start_idx[$start]] = array('text' => "<a href=\"srl-frame.php?verb=".$top->span->attribute."\" onclick=\"window.open(this.href, 'srlframe', 'width=400,height=400,scrollbars=yes,resizable=yes'); return false;\">V: ".$top->span->attributes["predicate"]."</a>", 'class' => 'V', 'span' => 1);
		$roles = predict_roles($top->span->attributes["predicate"]);
		foreach($top->children as $child => $label) {
			$child_node = $nodes[$child];
			$start = $child_node->span->start;
			$end = $child_node->span->end;

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
			$treeobj[$i][$start_idx[$start]] = array('text' => "<a href=\"srl-frame.php?verb=".$top->span->attributes["predicate"]."\" onclick=\"window.open(this.href, 'srlframe', 'width=400,height=400,scrollbars=yes,resizable=yes'); return false;\">".$roles[$lookup_label]." [".$label."]</a>", 'class' => $cl, 'start' => $start_idx[$start], 'span' => $rowspan);
		}
	}
	//$result .= "<pre>".print_r($record, true)."</pre>";
	$result .= "<table cellspacing=0 cellpadding=0>\n";
	$result .= "<tr><td></td><td></td>";
	foreach($forest->trees as $x) {
		$result .= "\t<td></td><td class='spacing'></td>\n";
	}
	$result .= "</tr>";
	foreach($record->tokens->labels as $i => $span) {
		$result .= "<tr>\n\t<td>".my_substr($text, $span->start, $span->end - $span->start)."</td><td class='spacing'></td>\n";
		for($j = 0; $j < count($treeobj); $j++) {
			if(array_key_exists($i, $treeobj[$j])) {
				if($treeobj[$j][$i]['span'] > 1) {
					$extra = " nobottom";
				} else {
					$extra = "";
				}
				$result .= "\t<td class=\"inspan ".$treeobj[$j][$i]['class'].$extra."\" valign=\"top\">".$treeobj[$j][$i]['text']."</td><td class='spacing'></td>\n";
			} else {
				if(array_key_exists($j, $span_class) && array_key_exists($i, $span_class[$j])) {
					if(array_key_exists($i+1, $span_class[$j]) && $span_class[$j][$i+1] == $span_class[$j][$i]) {
						$extra = " nobottom";
					} else {
						$extra = "";
					}
					$result .= "\t<td class=\"inspan ".$span_class[$j][$i]['class'].$extra."\">&nbsp;</td><td class='spacing'></td>\n";
				} else {
					$result .= "\t<td></td><td class='spacing'></td>\n";
				}
			}
		}
		$result .= "</tr>\n";
	}
	$result .= "</table>";
	return $result;
}

function predict_roles($verb) {
	$h = popen("../bin/role-predict.sh ".$verb, "r");
	$roles = array();
	while(!feof($h)) {
		$line = fgets($h);
		preg_match("/(\S+)\s+(.+)/", $line, $m);
		if(count($m) == 3) {
			$roles[$m[1]] = $m[2];
		}
	}
	pclose($h);
	return $roles;
}

?>