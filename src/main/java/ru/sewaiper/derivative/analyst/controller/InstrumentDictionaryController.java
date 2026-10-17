package ru.sewaiper.derivative.analyst.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import ru.sewaiper.derivative.analyst.domain.InstrumentPage;
import ru.sewaiper.derivative.analyst.domain.market.enums.AssetType;
import ru.sewaiper.derivative.analyst.service.dictionary.MarketDictionary;

import java.util.Set;

@RestController
@RequestMapping("/api/v1/instruments")
public class InstrumentDictionaryController {

    private final MarketDictionary dictionary;

    public InstrumentDictionaryController(MarketDictionary dictionary) {
        this.dictionary = dictionary;
    }

    @GetMapping("/asset-types")
    public Set<AssetType> supportedAssetTypes() {
        return dictionary.supportedAssetTypes();
    }

    @GetMapping("/{assetType}")
    public InstrumentPage search(@PathVariable AssetType assetType,
                                 @RequestParam(defaultValue = "") String query,
                                 @RequestParam(defaultValue = "0") int page,
                                 @RequestParam(defaultValue = "20") int size) {
        if (page < 0 || size < 1 || size > 50) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Page must be non-negative and size must be between 1 and 50");
        }
        return dictionary.search(assetType, query, page, size);
    }

    @PostMapping("/{assetType}/sync")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void sync(@PathVariable AssetType assetType) {
        if (!dictionary.supportedAssetTypes().contains(assetType)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Dictionary synchronization is unavailable for " + assetType);
        }
        dictionary.sync(assetType);
    }
}
